// TransactionAdapter.kt
package com.example.emptyviewactivity

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat

class TransactionAdapter(
    private val transactionList: MutableList<Transaction>,
    private val onDeleteClick: (Transaction) -> Unit // Lambda for delete action
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    // Formatter for currency display
    private val decimalFormat = DecimalFormat("'Rs.' #,##0.00")
    
    // Filtered list for display
    private var filteredList: MutableList<Transaction> = transactionList.toMutableList()
    
    // Current filter state
    private var currentFilter: FilterType = FilterType.ALL
    private var searchQuery: String = ""
    
    enum class FilterType {
        ALL, INCOME_ONLY, EXPENSE_ONLY
    }

    // ViewHolder class to hold the views for a single transaction item
    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDescription: TextView = itemView.findViewById(R.id.tvTransactionDescription)
        val tvAmount: TextView = itemView.findViewById(R.id.tvTransactionAmount)

        val categoryIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)

        val tvCategory: TextView = itemView.findViewById(R.id.tvTransactionCategory)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }

    // Called when RecyclerView needs a new ViewHolder of the given type
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    // Called by RecyclerView to display the data at the specified position
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = filteredList[position]
        val iconResId = getIconForCategory(transaction.category)

        holder.tvDescription.text = transaction.description
        holder.tvAmount.text = decimalFormat.format(transaction.amount)
        holder.tvCategory.text = transaction.category


        holder.categoryIcon.setImageResource(iconResId)


        // Set the color based on whether it's income or expense
        if (transaction.isIncome) {
            holder.tvAmount.setTextColor(Color.parseColor("#4CAF50")) // Green for Income
        } else {
            holder.tvAmount.setTextColor(Color.parseColor("#F44336")) // Red for Expense
        }

        // Set up the delete button functionality
        holder.btnDelete.setOnClickListener {
            onDeleteClick(transaction) // Execute the lambda passed from MainActivity
        }
    }

    private fun getIconForCategory(category: String): Int {
        return when (category) {
            "Food" -> R.drawable.ic_food
            "Transport" -> R.drawable.ic_transport
            "Shopping" -> R.drawable.ic_shopping
            "Bills" -> R.drawable.ic_bills
            "Entertainment" -> R.drawable.ic_entertainment
            else -> R.drawable.ic_others // A default icon
        }
    }

    // Returns the total number of items in the list
    override fun getItemCount(): Int = filteredList.size

    /**
     * Helper function to update the list and notify the RecyclerView.
     * This is useful if you were fetching data asynchronously, but here we
     * are mutating the list directly.
     */
    fun updateData() {
        applyFilters()
        notifyDataSetChanged()
    }
    
    /**
     * Filter transactions by description and type
     */
    fun filter(searchQuery: String, filterType: FilterType) {
        this.searchQuery = searchQuery.lowercase()
        this.currentFilter = filterType
        applyFilters()
        notifyDataSetChanged()
    }
    
    /**
     * Apply both search and type filters
     */
    private fun applyFilters() {
        filteredList.clear()
        
        for (transaction in transactionList) {
            // Apply type filter
            val matchesType = when (currentFilter) {
                FilterType.ALL -> true
                FilterType.INCOME_ONLY -> transaction.isIncome
                FilterType.EXPENSE_ONLY -> !transaction.isIncome
            }
            
            // Apply search filter
            val matchesSearch = searchQuery.isEmpty() || 
                transaction.description.lowercase().contains(searchQuery)
            
            if (matchesType && matchesSearch) {
                filteredList.add(transaction)
            }
        }
    }
    
    /**
     * Get the filtered list count (for showing "No results found")
     */
    fun getFilteredCount(): Int = filteredList.size
    
    /**
     * Update the original list and reapply filters
     */
    fun updateTransactionList(newList: MutableList<Transaction>) {
        transactionList.clear()
        transactionList.addAll(newList)
        applyFilters()
        notifyDataSetChanged()
    }
}