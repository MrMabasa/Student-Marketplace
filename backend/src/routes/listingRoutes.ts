
import { Router } from 'express';
import type { Request, Response, NextFunction, RequestHandler } from 'express';
import type { RowDataPacket, ResultSetHeader } from 'mysql2';
import multer from 'multer';
import { getDatabasePool } from '../config/database';
import { authenticateFirebase } from '../middleware/authenticateFirebase';
import { resolveDatabaseUser } from '../middleware/resolveDatabaseUser';

const router = Router();

interface ListingRow extends RowDataPacket {
  listing_id: number;
  seller_id: number;
  title: string;
  description: string;
  price: number;
  condition: string;
  category: string;
  module_code: string | null;
  status: string;
  date_created: Date | string;
  has_image: number;
  image_data?: Buffer | null;
  image_mime_type?: string | null;
}

function parseListingId(value: string): number | null {
  if (!/^\d+$/.test(value)) return null;
  const parsed = Number(value);
  return Number.isSafeInteger(parsed) && parsed > 0 ? parsed : null;
}

const listingSelect = `
  SELECT listing_id, seller_id, title, description, CAST(price AS DOUBLE) AS price,
         \`condition\` AS \`condition\`, category, module_code, status, date_created,
         CASE WHEN image_data IS NOT NULL AND OCTET_LENGTH(image_data) > 0 THEN 1 ELSE 0 END AS has_image
    FROM listings`;

// Store uploaded images in memory so they can be saved directly to MySQL.
const upload = multer({
  storage: multer.memoryStorage(),
  limits: {
    fileSize: 5 * 1024 * 1024,
    files: 1
  },
  fileFilter: (_request, file, callback) => {
    const allowedTypes = ['image/jpeg', 'image/png', 'image/webp'];

    if (!allowedTypes.includes(file.mimetype)) {
      callback(new Error('Only JPEG, PNG and WebP images are allowed.'));
      return;
    }

    callback(null, true);
  }
});

// Convert Multer errors into clear API responses.
const handleListingImageUpload: RequestHandler = (request, response, next) => {
  upload.single('image')(request, response, (error: unknown) => {
    if (error instanceof multer.MulterError) {
      const status = error.code === 'LIMIT_FILE_SIZE' ? 413 : 400;
      response.status(status).json({
        message: error.code === 'LIMIT_FILE_SIZE'
          ? 'The listing image must not exceed 5 MB.'
          : 'The listing image upload is invalid.'
      });
      return;
    }

    if (error instanceof Error) {
      response.status(400).json({ message: error.message });
      return;
    }

    next();
  });
};

// Create a listing for the authenticated marketplace user.
router.post(
  '/',
  authenticateFirebase,
  resolveDatabaseUser,
  handleListingImageUpload,
  async (request: Request, response: Response, next: NextFunction): Promise<void> => {
    const sellerId = request.databaseUserId;

    if (!sellerId) {
      response.status(401).json({ message: 'Authenticated marketplace user not found.' });
      return;
    }

    const title = typeof request.body.title === 'string'
      ? request.body.title.trim()
      : '';

    const description = typeof request.body.description === 'string'
      ? request.body.description.trim()
      : '';

    const category = typeof request.body.category === 'string'
      ? request.body.category.trim()
      : '';

    const condition = typeof request.body.condition === 'string'
      ? request.body.condition.trim()
      : '';

    const moduleCode = typeof request.body.module_code === 'string'
      ? request.body.module_code.trim() || null
      : null;

    const price = Number(request.body.price);

    if (!title || !description || !category || !condition) {
      response.status(400).json({
        message: 'Title, description, category and condition are required.'
      });
      return;
    }

    if (
      title.length > 255 ||
      category.length > 100 ||
      condition.length > 100 ||
      (moduleCode !== null && moduleCode.length > 100)
    ) {
      response.status(400).json({
        message: 'One or more listing fields exceed the allowed length.'
      });
      return;
    }

    if (!Number.isFinite(price) || price <= 0 || price > 99999999.99) {
      response.status(400).json({
        message: 'Enter a valid price greater than zero.'
      });
      return;
    }

    const imageData = request.file?.buffer ?? null;
    const imageMimeType = request.file?.mimetype ?? null;

    const connection = await getDatabasePool().getConnection();

    try {
      await connection.beginTransaction();

      const [result] = await connection.execute<ResultSetHeader>(
        `INSERT INTO listings
          (seller_id, title, description, price, \`condition\`,
           category, module_code, status, image_data, image_mime_type)
         VALUES (?, ?, ?, ?, ?, ?, ?, 'Active', ?, ?)`,
        [
          sellerId,
          title,
          description,
          price,
          condition,
          category,
          moduleCode,
          imageData,
          imageMimeType
        ]
      );

      const listingId = result.insertId;

      const [rows] = await connection.execute<ListingRow[]>(
        `${listingSelect} WHERE listing_id = ? LIMIT 1`,
        [listingId]
      );

      await connection.commit();

      response.status(201).json(rows[0]);
    } catch (error) {
      await connection.rollback();
      next(error);
    } finally {
      connection.release();
    }
  }
);

// Retrieve all listings.
router.get('/', async (_request, response) => {
  const [rows] = await getDatabasePool().execute<ListingRow[]>(
    `${listingSelect} ORDER BY date_created DESC, listing_id DESC`
  );
  response.status(200).json(rows);
});

// Retrieve a listing image.
router.get('/:id/image', async (request, response) => {
  const listingId = parseListingId(request.params.id);
  if (!listingId) {
    response.status(400).json({ message: 'Listing ID must be a positive integer.' });
    return;
  }

  const [rows] = await getDatabasePool().execute<ListingRow[]>(
    'SELECT image_data, image_mime_type FROM listings WHERE listing_id = ? LIMIT 1',
    [listingId]
  );

  const image = rows[0]?.image_data;

  if (!image || image.length === 0) {
    response.status(404).json({ message: 'Listing image not found.' });
    return;
  }

  const storedType = rows[0].image_mime_type;
  const contentType = storedType && /^image\/[a-z0-9.+-]+$/i.test(storedType)
    ? storedType
    : 'application/octet-stream';

  response.status(200).set('Content-Type', contentType).send(image);
});

// Retrieve an individual listing.
router.get('/:id', async (request, response) => {
  const listingId = parseListingId(request.params.id);

  if (!listingId) {
    response.status(400).json({ message: 'Listing ID must be a positive integer.' });
    return;
  }

  const [rows] = await getDatabasePool().execute<ListingRow[]>(
    `${listingSelect} WHERE listing_id = ? LIMIT 1`,
    [listingId]
  );

  if (rows.length === 0) {
    response.status(404).json({ message: 'Listing not found.' });
    return;
  }

  response.status(200).json(rows[0]);
});

export default router;
