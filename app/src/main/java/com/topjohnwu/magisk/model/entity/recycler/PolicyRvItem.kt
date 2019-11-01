package com.topjohnwu.magisk.model.entity.recycler

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import com.topjohnwu.magisk.R
import com.topjohnwu.magisk.databinding.ComparableRvItem
import com.topjohnwu.magisk.extensions.addOnPropertyChangedCallback
import com.topjohnwu.magisk.extensions.inject
import com.topjohnwu.magisk.extensions.toggle
import com.topjohnwu.magisk.model.entity.MagiskPolicy
import com.topjohnwu.magisk.model.events.PolicyEnableEvent
import com.topjohnwu.magisk.model.events.PolicyUpdateEvent
import com.topjohnwu.magisk.redesign.superuser.SuperuserViewModel
import com.topjohnwu.magisk.utils.KObservableField
import com.topjohnwu.magisk.utils.RxBus
import com.topjohnwu.magisk.utils.rotationTo
import com.topjohnwu.magisk.utils.setRevealed

class PolicyRvItem(val item: MagiskPolicy, val icon: Drawable) : ComparableRvItem<PolicyRvItem>() {

    override val layoutRes = R.layout.item_policy

    val isExpanded = KObservableField(false)
    val isEnabled = KObservableField(item.policy == MagiskPolicy.ALLOW)
    val shouldNotify = KObservableField(item.notification)
    val shouldLog = KObservableField(item.logging)

    fun toggle() = isExpanded.toggle()
    fun toggleNotify() = shouldNotify.toggle()
    fun toggleLog() = shouldLog.toggle()

    fun toggleEnabled() {
        if (isExpanded.value) {
            return
        }
        isEnabled.toggle()
    }

    fun toggle(view: View) {
        toggle()
        view.rotationTo(if (isExpanded.value) 225 else 180)
        (view.parent as ViewGroup)
            .findViewById<View>(R.id.expand_layout)
            .setRevealed(isExpanded.value)
    }

    private val rxBus: RxBus by inject()

    private val currentStateItem
        get() = item.copy(
            policy = if (isEnabled.value) MagiskPolicy.ALLOW else MagiskPolicy.DENY,
            notification = shouldNotify.value,
            logging = shouldLog.value
        )

    init {
        isEnabled.addOnPropertyChangedCallback {
            it ?: return@addOnPropertyChangedCallback
            rxBus.post(PolicyEnableEvent(this@PolicyRvItem, it))
        }
        shouldNotify.addOnPropertyChangedCallback {
            it ?: return@addOnPropertyChangedCallback
            rxBus.post(PolicyUpdateEvent.Notification(currentStateItem))
        }
        shouldLog.addOnPropertyChangedCallback {
            it ?: return@addOnPropertyChangedCallback
            rxBus.post(PolicyUpdateEvent.Log(currentStateItem))
        }
    }

    override fun contentSameAs(other: PolicyRvItem): Boolean = itemSameAs(other)
    override fun itemSameAs(other: PolicyRvItem): Boolean = item.uid == other.item.uid
}

class PolicyItem(val item: MagiskPolicy, val icon: Drawable) : ComparableRvItem<PolicyItem>() {
    override val layoutRes = R.layout.item_policy_md2

    val isExpanded = KObservableField(false)
    val isEnabled = KObservableField(item.policy == MagiskPolicy.ALLOW)
    val shouldNotify = KObservableField(item.notification)
    val shouldLog = KObservableField(item.logging)

    private val updatedPolicy
        get() = item.copy(
            policy = if (isEnabled.value) MagiskPolicy.ALLOW else MagiskPolicy.DENY,
            notification = shouldNotify.value,
            logging = shouldLog.value
        )

    fun toggle(viewModel: SuperuserViewModel) {
        if (isExpanded.value) {
            return
        }
        isEnabled.toggle()
        viewModel.togglePolicy(this, isEnabled.value)
    }

    fun toggle(view: View) {
        isExpanded.toggle()
        view.rotationTo(if (isExpanded.value) 225 else 180)
        (view.parent as ViewGroup)
            .findViewById<View>(R.id.expand_layout)
            .setRevealed(isExpanded.value)
    }

    fun toggleNotify(viewModel: SuperuserViewModel) {
        shouldNotify.toggle()
        viewModel.updatePolicy(PolicyUpdateEvent.Notification(updatedPolicy))
    }

    fun toggleLog(viewModel: SuperuserViewModel) {
        shouldLog.toggle()
        viewModel.updatePolicy(PolicyUpdateEvent.Log(updatedPolicy))
    }

    override fun contentSameAs(other: PolicyItem) = itemSameAs(other)
    override fun itemSameAs(other: PolicyItem) = item.uid == other.item.uid

}