package com.iptvplayer.m3u.stream.ui.iap

import android.content.Context
import android.util.Log
import android.util.TypedValue
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.isVisible
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.iptvplayer.m3u.stream.R
import com.iptvplayer.m3u.stream.databinding.FragmentIAPBinding
import com.iptvplayer.m3u.stream.utils.ExitSubscriptionDialog
import com.iptvplayer.m3u.stream.utils.invisible
import com.iptvplayer.m3u.stream.utils.visible
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.onBackPressed
import hoang.dqm.codebase.base.activity.popBackStack
import hoang.dqm.codebase.utils.AppMonetization
import hoang.dqm.codebase.utils.billing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import tpt.dev.monetization.subs.listener.BillingClientListener
import tpt.dev.monetization.subs.listener.SubscriptionServiceListener
import tpt.dev.monetization.subs.model.IAPProduct
import tpt.dev.monetization.subs.model.PurchaseInfo
import tpt.dev.monetization.subs.model.priceAmountMicros

typealias ProductWithSelection = Pair<IAPProduct, Boolean>

class IAPFragment : BaseFragment<FragmentIAPBinding, IAPViewModel>(),
    BillingClientListener,
    SubscriptionServiceListener {

    private val billingManager by lazy { AppMonetization.billing }
    private val isBillingClientConnectedFlow = MutableStateFlow(billingManager.isConnected())
    private val pricedProductsFlow = MutableStateFlow(billingManager.getPricedProducts())
    private val selectedProductIdFlow = MutableStateFlow<String?>(null)
    private val productAdapter by lazy {
        IAPProductAdapter()
    }

    private var selectedProduct: IAPProduct? = null
    private var displayedProducts: List<IAPProduct> = emptyList()

    private val isFromSplash by lazy {
        arguments?.getBoolean("isFromSplash") ?: false
    }

    companion object {
        private const val CLOSE_BUTTON_DELAY_MS = 3000L
    }

    // ─── BillingClientListener ────────────────────────────────────────────────

    override fun onConnected(isConnected: Boolean, responseCode: Int) {
        isBillingClientConnectedFlow.tryEmit(isConnected)
    }

    override fun onQueryProductDetailComplete(products: List<IAPProduct>) {
        android.util.Log.d("IAP_DEBUG", "==== ALL PRODUCTS FROM GOOGLE PLAY ====")

        products.forEach {
            android.util.Log.d(
                "IAP_DEBUG",
                "productId=${it.productId}, price=${it.priceAmountMicros()}, freeTrialDays=${it.freeTrialDays}"
            )
        }

        pricedProductsFlow.tryEmit(products)
    }

    override fun onLaunchPurchaseComplete(isSuccess: Boolean) {
        if (!isSuccess) {
            Toast.makeText(
                requireContext(),
                getString(R.string.text_an_error_occurred_please_try_again_later),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ─── SubscriptionServiceListener ─────────────────────────────────────────

    override fun onSubscriptionRestored(purchaseInfo: PurchaseInfo) {
        showUpgradeSuccessDialog()
    }

    override fun onSubscriptionPurchased(purchaseInfo: PurchaseInfo) {
        showUpgradeSuccessDialog()
    }

    override fun onSubscriptionPurchasePending(purchaseInfo: PurchaseInfo) {
        // no-op – optionally show a "pending" message
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun initView() {
        Log.d("SplashFragment", "isFromSplash: $isFromSplash")
        adjustInsetsForBottomNavigation(binding.bg)

        binding.titleAccess.text = buildSpannedString {
            color("#037EB9".toColorInt()) { append(getString(R.string.text_premium)) }
            append(" ")
            color("#000000".toColorInt()) { append(getString(R.string.text_access)) }
        }

        setupCloseButtonDelay()
        productAdapter.setOnClickItem { product, position ->
                selectedProductIdFlow.tryEmit(product.productId)
        }
        binding.rvProducts.run {
            adapter = productAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }

        updateSelectedProductUi()
    }

    override fun initListener() {
        // Back-press & close
        binding.btnClose.setOnClickListener { handleClose() }
        onBackPressed { handleClose() }

        // Continue / buy
        binding.btnSave.setOnClickListener {
            if (!isBillingClientConnectedFlow.value) {
                context?.let {
                    Toast.makeText(
                        it,
                        getString(R.string.text_waiting_for_billing_connection),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@setOnClickListener
            }

            val product = selectedProduct
            if (product == null) {
                context?.let {
                    Toast.makeText(
                        it,
                        getString(R.string.text_no_product_selected),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@setOnClickListener
            }

            billingManager.buyBasePlan(requireActivity(), product)
        }

        // Trial toggle (if your layout has a toggle switch like SubscriptionActivity)
//        binding.cardTrialToggle?.setOnClickListener {
//            binding.ivSwitch?.isChecked = binding.ivSwitch?.isChecked?.not() ?: false
//        }
//        binding.ivSwitch?.setOnCheckedChangeListener { _, _ ->
//            toggleTrialSelection()
//        }
    }

    override fun initData() {
        listenBillingManager()
    }

    override fun onDestroyView() {
        billingManager.removeBillingClientListener(this)
        billingManager.removeSubscriptionListener(this)
        super.onDestroyView()
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private fun listenBillingManager() {
        android.util.Log.d("IAP_DEBUG", "=== listenBillingManager START ===")
        android.util.Log.d("IAP_DEBUG", "isConnected=${billingManager.isConnected()}")
        android.util.Log.d("IAP_DEBUG", "getPricedProducts size=${billingManager.getPricedProducts().size}")
        
        billingManager.getPricedProducts().forEach {
            android.util.Log.d(
                "IAP_DEBUG",
                "EXISTING productId=${it.productId}, price=${it.priceAmountMicros()}, freeTrialDays=${it.freeTrialDays}"
            )
        }
        
        billingManager.addBillingClientListener(this)
        billingManager.addSubscriptionListener(this)

        val displayProductIds = listOf(
            getString(R.string.billing_sub_year),
            getString(R.string.billing_sub_week)
        )

        val iapProductsFlow = pricedProductsFlow.map { products ->
            products
                .filter { displayProductIds.contains(it.productId) }
                .sortedBy { displayProductIds.indexOf(it.productId) }
        }

        selectedProductIdFlow
            .combine(iapProductsFlow) { selectedId, items ->
                val resolvedId = items.firstOrNull { it.productId == selectedId }?.productId
                    ?: items.firstOrNull { it.freeTrialDays > 0 }?.productId
                    ?: items.firstOrNull()?.productId

                items.map { it to (it.productId == resolvedId) }
            }
            .asLiveData()
            .observe(viewLifecycleOwner) { products ->
                displayedProducts = products.map { it.first }
                selectedProduct = products.firstOrNull { it.second }?.first
                productAdapter.setList(products)
                Log.d("IAP_DEBUG", "listenBillingManager: ${products.size}")
                binding.rvProducts.isVisible = products.isNotEmpty()
                binding.layoutLoading.isVisible =
                    products.isEmpty() || !isBillingClientConnectedFlow.value
                updateSelectedProductUi()
            }

        isBillingClientConnectedFlow
            .asLiveData()
            .observe(viewLifecycleOwner) { isConnected ->
                binding.layoutLoading.isVisible = !isConnected || displayedProducts.isEmpty()
                if (!isConnected) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.text_waiting_for_billing_connection),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                updateSelectedProductUi()
            }
    }

    private fun setupCloseButtonDelay() {
        binding.btnClose.invisible()
        binding.btnClose.postDelayed({
            if (isAdded && !isDetached && view != null) {
                binding.btnClose.visible()
            }
        }, CLOSE_BUTTON_DELAY_MS)
    }

    private fun toggleTrialSelection() {
        val hasFreeTrialSelected = (selectedProduct?.freeTrialDays ?: 0) > 0
        val target = if (hasFreeTrialSelected) {
            displayedProducts.firstOrNull { it.freeTrialDays == 0 }
        } else {
            displayedProducts.firstOrNull { it.freeTrialDays > 0 }
        }
        target?.let { selectedProductIdFlow.tryEmit(it.productId) }
    }

    private fun updateSelectedProductUi() {
        val hasFreeTrialSelected = (selectedProduct?.freeTrialDays ?: 0) > 0

        binding.btnSave.text = getString(
            if (hasFreeTrialSelected) R.string.text_start_free_now else R.string.text_start_watching_now
        )

        val isEnabled = isBillingClientConnectedFlow.value && selectedProduct != null
        binding.btnSave.isEnabled = isEnabled
        binding.btnSave.alpha = if (isEnabled) 1f else 0.75f
    }

    private fun handleClose() {
        if (isFromSplash) {
            ExitSubscriptionDialog(
                requireActivity(),
                onConfirm = {
                    val freeTrialProduct = displayedProducts.firstOrNull { it.freeTrialDays > 0 }

                    if (freeTrialProduct == null) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.text_no_product_selected),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@ExitSubscriptionDialog
                    }

                    if (!isBillingClientConnectedFlow.value) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.text_waiting_for_billing_connection),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@ExitSubscriptionDialog
                    }

                    selectedProductIdFlow.tryEmit(freeTrialProduct.productId)
                    selectedProduct = freeTrialProduct
                    billingManager.buyBasePlan(requireActivity(), freeTrialProduct)
                },
                onCancel = {
                    popBackStack()
                }
            ).show()
        } else {
            popBackStack()
        }
    }

    private fun showUpgradeSuccessDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.text_congratulation))
            .setMessage(getString(R.string.text_you_have_successfully_upgraded_to_premium))
            .setPositiveButton(getString(R.string.text_ok)) { _, _ -> popBackStack() }
            .show()
    }

    fun Context.getAttrColor(attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }
}