package com.victorkirui.remindly.widget

import androidx.glance.action.ActionParameters

object WidgetIntentConstants {
    const val EXTRA_OPEN_CAPTURE = "extra_open_capture"
    const val EXTRA_SELECTED_DATE = "extra_selected_date"
    const val EXTRA_ITEM_ID = "extra_item_id"
    const val EXTRA_OPEN_PAYWALL = "extra_open_paywall"

    val PARAM_OPEN_CAPTURE = ActionParameters.Key<Boolean>(EXTRA_OPEN_CAPTURE)
    val PARAM_SELECTED_DATE = ActionParameters.Key<String>(EXTRA_SELECTED_DATE)
    val PARAM_ITEM_ID = ActionParameters.Key<String>(EXTRA_ITEM_ID)
    val PARAM_OPEN_PAYWALL = ActionParameters.Key<Boolean>(EXTRA_OPEN_PAYWALL)
}
