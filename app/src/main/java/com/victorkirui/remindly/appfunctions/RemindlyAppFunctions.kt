package com.victorkirui.remindly.appfunctions

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext
import com.victorkirui.core.model.ShareContent
import com.victorkirui.core.repository.SubscriptionRepository
import com.victorkirui.core.repository.SubscriptionTier
import com.victorkirui.core.repository.AuthRepository
import com.victorkirui.module_features.capturing.CapturingUseCase
import com.victorkirui.module_features.capturing.CaptureResult
import com.victorkirui.local.repository.LocalRepository
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RemindlyAppFunctions : KoinComponent {

    private val capturingUseCase: CapturingUseCase by inject()
    private val subscriptionRepository: SubscriptionRepository by inject()
    private val localRepository: LocalRepository by inject()
    private val authRepository: AuthRepository by inject()

    /**
     * Creates a new reminder or capture in Remindly from text.
     *
     * @param context The app function context.
     * @param text The text to capture (e.g., "Remind me to buy milk tomorrow").
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun createReminder(context: AppFunctionContext, text: String): String {
        val tier = subscriptionRepository.subscriptionTier.first()
        if (tier != SubscriptionTier.PREMIUM) {
            return "This feature is available for Premium users only."
        }

        val result = capturingUseCase(ShareContent.Text(text, source = "Gemini"))
        return when (result) {
            is CaptureResult.Success -> "Successfully saved to Remindly."
            is CaptureResult.Duplicate -> "This is already in your Remindly."
            is CaptureResult.SavedLocallyOnly -> "Saved offline. I'll process it when you're back online."
            is CaptureResult.UpgradeRequired -> "You've reached your free limit. Upgrade to Premium to save more."
            else -> "I couldn't save that to Remindly right now."
        }
    }

    /**
     * Searches for saved captures in Remindly.
     *
     * @param context The app function context.
     * @param query The search term.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun searchCaptures(context: AppFunctionContext, query: String): List<String> {
        val tier = subscriptionRepository.subscriptionTier.first()
        if (tier != SubscriptionTier.PREMIUM) {
            return listOf("Search via Gemini is a Premium feature.")
        }

        val userId = authRepository.currentUser.value?.uid ?: ""
        
        if (userId.isEmpty()) return listOf("You need to be logged in to search.")

        val items = localRepository.searchItems(userId, query).first()
        if (items.isEmpty()) return listOf("No items found matching \"$query\".")
        
        return items.take(5).map { 
            "${it.title}${if (it.category != null) " [${it.category}]" else ""}" 
        }
    }
}
