package com.studentmarketplace.data.remote

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class FirebaseAuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val user = FirebaseAuth.getInstance().currentUser
            ?: return chain.proceed(request)

        val tokenResult = try {
            Tasks.await(user.getIdToken(false), 15, TimeUnit.SECONDS)
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException("Could not retrieve Firebase authentication token.", error)
        } catch (error: Exception) {
            throw IOException("Could not retrieve Firebase authentication token.", error)
        }
        val token = tokenResult.token
            ?: throw IOException("Firebase did not return an authentication token.")

        val authenticatedRequest = request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(authenticatedRequest)
    }
}