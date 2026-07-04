package com.iptvplayer.m3u.stream.ui.iap

import android.util.Log
import com.android.billingclient.api.ProductDetails
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.ItemPurchaseBinding
import com.iptvplayer.m3u.stream.utils.gone
import com.iptvplayer.m3u.stream.utils.visible
import hoang.dqm.codebase.base.adapter.BaseRecyclerViewAdapter
import hoang.dqm.codebase.utils.getColorFromRes
import tpt.dev.monetization.subs.extensions.biggestPrice
import tpt.dev.monetization.subs.extensions.biggestSubscriptionOfferDetailsToken
import tpt.dev.monetization.subs.model.IAPProduct
import tpt.dev.monetization.subs.model.IAPProductPeriods
import tpt.dev.monetization.subs.model.periods
import tpt.dev.monetization.subs.model.priceAmountMicros
import tpt.dev.monetization.subs.model.priceCurrencyCode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

class IAPProductAdapter: BaseRecyclerViewAdapter<ProductWithSelection, ItemPurchaseBinding>(){
    override fun bindData(
        binding: ItemPurchaseBinding,
        item: ProductWithSelection,
        position: Int
    ) {

        val (product, isSelected) = item

        Log.d("IAP_BIND", "==============================")
        Log.d("IAP_BIND", "POS = $position")
        Log.d("IAP_BIND", "PRODUCT_ID = ${product.productId}")
        Log.d("IAP_BIND", "SELECTED = $isSelected")
        Log.d("IAP_BIND", "PERIOD = ${product.periods()}")

        val regularPrice = product.productDetails
            ?.subscriptionOfferDetails
            ?.flatMap { it.pricingPhases.pricingPhaseList }
            ?.find {
                it.recurrenceMode == ProductDetails.RecurrenceMode.INFINITE_RECURRING
            }
            ?.formattedPrice
            .orEmpty()

        Log.d("IAP_BIND", "PRICE = $regularPrice")

        val title = getDisplayTitle(product)
        val name = getNamePeriodPage(product)
        val weekly = getWeeklyPrice(product)

        Log.d("IAP_BIND", "TITLE = $title")
        Log.d("IAP_BIND", "NAME = $name")
        Log.d("IAP_BIND", "WEEKLY_PRICE = $weekly")

        val isWeekly = product.periods() == IAPProductPeriods.Weekly
        val isYearly = product.periods() == IAPProductPeriods.Yearly

        Log.d("IAP_BIND", "IS_WEEKLY = $isWeekly")
        Log.d("IAP_BIND", "IS_YEARLY = $isYearly")

        // ===== UI BIND =====

        binding.tvDescription.text = title
        binding.tvNameProduct.text = name
        binding.textPrice.text = regularPrice
        binding.textPriceDes.text = weekly

        if (isWeekly) {
            Log.d("IAP_BIND", "HIDE WEEKLY PRICE")
            binding.textPriceDes.gone()
        } else {
            binding.textPriceDes.visible()
        }

        if (isYearly) {
            Log.d("IAP_BIND", "SHOW BEST DEAL")
            binding.bestDeal.text = context.getString(R.string.text_best_deal)
            binding.bestDeal.visible()
        } else {
            binding.bestDeal.gone()
        }

        // ===== CLICK =====
        binding.root.setOnClickListener {
            Log.d("IAP_BIND", "CLICKED → $product.productId (pos=$position)")
            clickListener?.invoke(product, position)
        }

        // ===== SELECTION STATE =====
        if (isSelected) {
            Log.d("IAP_BIND", "STATE → SELECTED (${product.productId})")

            binding.btnIap1.setBackgroundResource(R.drawable.box_bg_iap_w)
            binding.textPrice.setTextColor(getColorFromRes(R.color.color_primary))
        } else {
            Log.d("IAP_BIND", "STATE → NORMAL (${product.productId})")

            binding.btnIap1.setBackgroundResource(R.drawable.box_bg_iap_unselected_w)
            binding.textPrice.setTextColor(getColorFromRes(R.color.black))
        }
    }

    private fun getTimePeriodPage(product: IAPProduct): String = when (product.periods()) {
        IAPProductPeriods.Weekly -> context.getString(R.string.text_week)
        IAPProductPeriods.Monthly -> context.getString(R.string.text_month)
        IAPProductPeriods.Yearly -> context.getString(R.string.text_year)
        else -> ""
    }

    fun getWeeklyPrice(product: IAPProduct): String {
        val phase = product.productDetails
            ?.subscriptionOfferDetails
            ?.flatMap { it.pricingPhases.pricingPhaseList }
            ?.find {
                it.recurrenceMode == ProductDetails.RecurrenceMode.INFINITE_RECURRING
            } ?: return ""

        val weeklyMicros = when {
            phase.billingPeriod.contains("W") -> phase.priceAmountMicros.toDouble()
            phase.billingPeriod.contains("M") -> (phase.priceAmountMicros / 4).toDouble()
            phase.billingPeriod.contains("Y") -> (phase.priceAmountMicros / 50).toDouble()
            else -> phase.priceAmountMicros.toDouble()
        }

        val formatted = formatCurrency(
            weeklyMicros,
            phase.priceCurrencyCode
        ) ?: return ""

        return "≈ $formatted/week"
    }


    private fun getNamePeriodPage(product: IAPProduct): String = when (product.periods()) {
        IAPProductPeriods.Weekly -> context.getString(R.string.text_weekly)
        IAPProductPeriods.Monthly -> context.getString(R.string.text_monthly)
        IAPProductPeriods.Yearly -> context.getString(R.string.text_yearly)
        else -> ""
    }

    private fun getDisplayTitle(product: IAPProduct): String {
        if (product.isOneTime) return context.getString(R.string.text_onetime)
        return if (product.freeTrialDays > 0) {
            if (product.freeTrialDays == 3) {
                context.getString(R.string.text_3_days_free_trial)
            } else {
                context.getString(R.string.free_trial_name, product.freeTrialDays.toString())
            }
        } else {
            context.getString(R.string.text_save_up_to_99)
        }
    }

    private fun getTrailingPrice(product: IAPProduct): String? {
        return when (product.periods()) {
            IAPProductPeriods.Yearly -> {
                val weeklyMicros = product.priceAmountMicros()?.div(50.0)
                formatCurrency(weeklyMicros, product.priceCurrencyCode())
            }

            else -> {
                product.productDetails
                    ?.biggestSubscriptionOfferDetailsToken()
                    ?.biggestPrice()
                    ?.formattedPrice
            }
        }
    }


    private fun formatCurrency(priceMicros: Double?, currencyCode: String?): String? {
        if (priceMicros == null) return null
        return runCatching {
            val amount = priceMicros / 1_000_000.0
            val isWholeNumber = abs(amount - amount.roundToLong().toDouble()) < 0.000001
            NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
                if (!currencyCode.isNullOrBlank()) {
                    currency = Currency.getInstance(currencyCode)
                }
                minimumFractionDigits = 0
                maximumFractionDigits = if (isWholeNumber) 0 else 2
            }.format(amount)
        }.getOrNull()
    }
    private var clickListener: ((item: IAPProduct, position: Int) -> Unit)? = null

    fun setOnClickItem(listener: (item: IAPProduct, position: Int) -> Unit){
        clickListener = listener
    }

}