CREATE TABLE IF NOT EXISTS purchase_requests (
    request_id INT NOT NULL AUTO_INCREMENT,
    listing_id INT NOT NULL,
    buyer_id INT NOT NULL,
    seller_id INT NOT NULL,

    status ENUM(
        'Pending',
        'Accepted',
        'Declined',
        'Cancelled'
    ) NOT NULL DEFAULT 'Pending',

    date_requested DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_updated DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (request_id),

    INDEX idx_requests_buyer (buyer_id),
    INDEX idx_requests_seller (seller_id),
    INDEX idx_requests_listing (listing_id),
    INDEX idx_requests_status (status),

    CONSTRAINT fk_requests_listing
        FOREIGN KEY (listing_id)
        REFERENCES listings(listing_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_requests_buyer
        FOREIGN KEY (buyer_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_requests_seller
        FOREIGN KEY (seller_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE
);