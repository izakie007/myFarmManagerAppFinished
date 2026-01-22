package com.palmfarm.manager.utils

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

/**
 * Extension functions for common operations
 */

// Context Extensions

/**
 * Show a short toast message
 */
fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

/**
 * Show a long toast message
 */
fun Context.showLongToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

// View Extensions

/**
 * Make view visible
 */
fun View.visible() {
    visibility = View.VISIBLE
}

/**
 * Make view invisible (still takes up space)
 */
fun View.invisible() {
    visibility = View.INVISIBLE
}

/**
 * Make view gone (doesn't take up space)
 */
fun View.gone() {
    visibility = View.GONE
}

/**
 * Toggle view visibility
 */
fun View.toggleVisibility() {
    visibility = if (visibility == View.VISIBLE) View.GONE else View.VISIBLE
}

/**
 * Show or hide view based on condition
 */
fun View.visibleIf(condition: Boolean) {
    visibility = if (condition) View.VISIBLE else View.GONE
}

/**
 * Hide keyboard from view
 */
fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.hideSoftInputFromWindow(windowToken, 0)
}

/**
 * Show keyboard for view
 */
fun View.showKeyboard() {
    requestFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

/**
 * Show snackbar
 */
fun View.showSnackbar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
    Snackbar.make(this, message, duration).show()
}

/**
 * Show snackbar with action
 */
fun View.showSnackbarWithAction(
    message: String,
    actionText: String,
    action: () -> Unit,
    duration: Int = Snackbar.LENGTH_LONG
) {
    Snackbar.make(this, message, duration)
        .setAction(actionText) { action() }
        .show()
}

// Fragment Extensions

/**
 * Show toast from fragment
 */
fun Fragment.showToast(message: String) {
    context?.showToast(message)
}

/**
 * Show long toast from fragment
 */
fun Fragment.showLongToast(message: String) {
    context?.showLongToast(message)
}

/**
 * Hide keyboard from fragment
 */
fun Fragment.hideKeyboard() {
    view?.hideKeyboard()
}

// String Extensions

/**
 * Check if string is a valid number
 */
fun String.isNumeric(): Boolean {
    return this.toDoubleOrNull() != null
}

/**
 * Capitalize first letter of each word
 */
fun String.capitalizeWords(): String {
    return split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { it.uppercase() }
    }
}

/**
 * Truncate string to max length with ellipsis
 */
fun String.truncate(maxLength: Int): String {
    return if (length <= maxLength) {
        this
    } else {
        "${take(maxLength - 3)}..."
    }
}

// Double Extensions

/**
 * Format as XAF currency
 */
fun Double.toXAF(): String {
    return CurrencyUtils.formatAmount(this)
}

/**
 * Format as quantity
 */
fun Double.toQuantityString(): String {
    return CurrencyUtils.formatQuantity(this)
}

/**
 * Format as percentage
 */
fun Double.toPercentageString(): String {
    return CurrencyUtils.formatPercentage(this)
}

/**
 * Round to 2 decimal places
 */
fun Double.roundToTwo(): Double {
    return CurrencyUtils.roundToTwoDecimals(this)
}

// Long Extensions (Timestamp)

/**
 * Format timestamp to display date
 */
fun Long.toDisplayDate(): String {
    return DateUtils.formatToDisplay(this)
}

/**
 * Format timestamp to short date
 */
fun Long.toShortDate(): String {
    return DateUtils.formatToShort(this)
}

/**
 * Format timestamp to "time ago" format
 */
fun Long.toTimeAgo(): String {
    return DateUtils.formatTimeAgo(this)
}

// Int Extensions

/**
 * Get month name from month number (1-12)
 */
fun Int.toMonthName(): String {
    return DateUtils.getMonthName(this)
}

// Collection Extensions

/**
 * Sum of doubles in list
 */
fun List<Double>.sumOfDoubles(): Double {
    return sumOf { it }
}

/**
 * Calculate average of doubles
 */
fun List<Double>.averageOfDoubles(): Double {
    return if (isEmpty()) 0.0 else sumOf { it } / size
}

/**
 * Safe get element or null
 */
fun <T> List<T>.getOrNull(index: Int): T? {
    return if (index in indices) this[index] else null
}

// Nullable Extensions

/**
 * Return value or default if null
 */
fun <T> T?.orDefault(default: T): T {
    return this ?: default
}

/**
 * Execute block if not null
 */
inline fun <T> T?.ifNotNull(block: (T) -> Unit) {
    this?.let(block)
}

// Boolean Extensions

/**
 * Execute block if true
 */
inline fun Boolean.ifTrue(block: () -> Unit) {
    if (this) block()
}

/**
 * Execute block if false
 */
inline fun Boolean.ifFalse(block: () -> Unit) {
    if (!this) block()
}
