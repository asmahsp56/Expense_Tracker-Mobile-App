package com.example.emptyviewactivity

import android.os.Bundle
import android.content.Context
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import com.example.emptyviewactivity.TransactionAdapter.FilterType
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.view.LayoutInflater
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private lateinit var tvTotalAmount: TextView
    private lateinit var rvTransactions: RecyclerView
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var toolbar: Toolbar
    private lateinit var tvNoResults: TextView
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalExpense: TextView
    private lateinit var tvIncomeCount: TextView
    private lateinit var tvExpenseCount: TextView
    private lateinit var fabAddTransaction: FloatingActionButton
    private var transactionList = mutableListOf<Transaction>()
    private var currentFilterType: FilterType = FilterType.ALL
    private var currentSearchQuery: String = ""

    private val decimalFormat = DecimalFormat("'Rs.' #,##0.00")

    // Define keys for SharedPreferences
    private val PREFS_NAME = "ExpenseTrackerPrefs"
    private val TRANSACTIONS_KEY = "transactions"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        init()
        setupToolbar()
        loadData()
        updateBalance()
        updateIncomeExpenseSummary()

    }


    fun init()
    {
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        rvTransactions = findViewById(R.id.rvTransactions)
        tvNoResults = findViewById(R.id.tvNoResults)
        tvTotalIncome = findViewById(R.id.tvTotalIncome)
        tvTotalExpense = findViewById(R.id.tvTotalExpense)
        tvIncomeCount = findViewById(R.id.tvIncomeCount)
        tvExpenseCount = findViewById(R.id.tvExpenseCount)
        fabAddTransaction = findViewById(R.id.fabAddTransaction)

        transactionAdapter = TransactionAdapter(transactionList) { transaction ->
            deleteTransaction(transaction) // Pass the delete logic
        }
        rvTransactions.adapter = transactionAdapter
        rvTransactions.layoutManager = LinearLayoutManager(this)
        
        updateNoResultsVisibility()

        fabAddTransaction.setOnClickListener {
            showAddTransactionDialog()
        }

    }
    
    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    private fun showAddTransactionDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_transaction, null)

        val actvCategory = dialogView.findViewById<AutoCompleteTextView>(R.id.actvCategory)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.etDescription)
        val etAmount = dialogView.findViewById<TextInputEditText>(R.id.etAmount)
        val btnAddIncome = dialogView.findViewById<Button>(R.id.btnAddIncome)
        val btnAddExpense = dialogView.findViewById<Button>(R.id.btnAddExpense)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()


        btnAddIncome.setOnClickListener {

            val incomeCategories = resources.getStringArray(R.array.income_categories)
            actvCategory.setAdapter(
                ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, incomeCategories)
            )

            addTransactionFromDialog(actvCategory, etDescription, etAmount, true, dialog)
        }


        btnAddExpense.setOnClickListener {

            val expenseCategories = resources.getStringArray(R.array.expense_categories)
            actvCategory.setAdapter(
                ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, expenseCategories)
            )

            addTransactionFromDialog(actvCategory, etDescription, etAmount, false, dialog)
        }

        dialog.show()
    }


    private fun addTransactionFromDialog(
        actvCategory: AutoCompleteTextView,
        etDescription: TextInputEditText,
        etAmount: TextInputEditText,
        isIncome: Boolean,
        dialog: AlertDialog
    ) {
        val description = etDescription.text?.toString()?.trim() ?: ""
        val amountTxt = etAmount.text?.toString()?.trim() ?: ""

        // Set categories dynamically based on type
        val categories = if (isIncome) {
            listOf("Salary", "Freelance", "Investment", "Gift", "Other")
        } else {
            listOf("Food", "Transport", "Shopping", "Bills", "Entertainment", "Health", "Education", "Other")
        }


        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        actvCategory.setAdapter(arrayAdapter)

        val category = actvCategory.text.toString().trim()

        if (category.isEmpty()) {
            Toast.makeText(this, "Category cannot be empty", Toast.LENGTH_LONG).show()
            actvCategory.requestFocus()
            return
        }

        if (description.isEmpty() || amountTxt.isEmpty()) {
            Toast.makeText(this, "Description and amount cannot be empty", Toast.LENGTH_LONG).show()
            etDescription.requestFocus()
            return
        }

        val amount = amountTxt.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Please enter a valid amount greater than zero", Toast.LENGTH_LONG).show()
            etAmount.requestFocus()
            return
        }

        // Create new transaction
        val newTransaction = Transaction(
            id = System.currentTimeMillis(),
            description = description,
            amount = amount,
            isIncome = isIncome,
            category = category
        )

        transactionList.add(0, newTransaction)


        transactionAdapter.updateTransactionList(transactionList)
        updateNoResultsVisibility()
        rvTransactions.scrollToPosition(0)
        updateBalance()
        updateIncomeExpenseSummary()

        val type = if (isIncome) "Income" else "Expense"
        Toast.makeText(this, "$type of $amount added successfully", Toast.LENGTH_SHORT).show()

        dialog.dismiss()
    }


    fun updateBalance()
    {
        var total = 0.0
        for(transaction in transactionList){
            total += ( if(transaction.isIncome) transaction.amount else -transaction.amount)
        }

        tvTotalAmount.text = decimalFormat.format(total)
    }
    
    fun updateIncomeExpenseSummary()
    {
        var totalIncome = 0.0
        var totalExpense = 0.0
        var incomeCount = 0
        var expenseCount = 0
        
        for(transaction in transactionList){
            if(transaction.isIncome) {
                totalIncome += transaction.amount
                incomeCount++
            } else {
                totalExpense += transaction.amount
                expenseCount++
            }
        }
        

        tvTotalIncome.text = decimalFormat.format(totalIncome)
        tvIncomeCount.text = getString(R.string.label_transactions_count, incomeCount)
        

        tvTotalExpense.text = decimalFormat.format(totalExpense)
        tvExpenseCount.text = getString(R.string.label_transactions_count, expenseCount)
    }

    fun deleteTransaction(transaction: Transaction){
        val ind = transactionList.indexOf(transaction)

        if(ind >= 0){
            transactionList.removeAt(ind)

            // Update adapter with new list and reapply filters
            transactionAdapter.updateTransactionList(transactionList)
            updateNoResultsVisibility()

            updateBalance()
            updateIncomeExpenseSummary()

            Toast.makeText(this,"Transaction Delete: ${transaction.description}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        
        // Set up SearchView
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView
        
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText ?: ""
                transactionAdapter.filter(currentSearchQuery, currentFilterType)
                updateNoResultsVisibility()
                return true
            }
        })
        
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_filter -> {
                showFilterDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showFilterDialog() {
        val filterOptions = arrayOf(
            getString(R.string.filter_all),
            getString(R.string.filter_income),
            getString(R.string.filter_expense)
        )
        
        val checkedItem = when (currentFilterType) {
            FilterType.ALL -> 0
            FilterType.INCOME_ONLY -> 1
            FilterType.EXPENSE_ONLY -> 2
        }
        
        AlertDialog.Builder(this)
            .setTitle("Filter Transactions")
            .setSingleChoiceItems(filterOptions, checkedItem) { dialog, which ->
                currentFilterType = when (which) {
                    0 -> FilterType.ALL
                    1 -> FilterType.INCOME_ONLY
                    2 -> FilterType.EXPENSE_ONLY
                    else -> FilterType.ALL
                }
                transactionAdapter.filter(currentSearchQuery, currentFilterType)
                updateNoResultsVisibility()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun updateNoResultsVisibility() {
        if (transactionAdapter.getFilteredCount() == 0) {
            tvNoResults.visibility = TextView.VISIBLE
            rvTransactions.visibility = RecyclerView.GONE
        } else {
            tvNoResults.visibility = TextView.GONE
            rvTransactions.visibility = RecyclerView.VISIBLE
        }
    }

    private fun loadData() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString(TRANSACTIONS_KEY, null)
        val type = object : TypeToken<MutableList<Transaction>>() {}.type

        transactionList = if (json != null) {
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }
        
        // Update adapter with loaded data and apply filters
        if (::transactionAdapter.isInitialized) {
            transactionAdapter.updateTransactionList(transactionList)
            updateNoResultsVisibility()
        }
    }

    private fun saveData() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        // Convert the list of transactions to a JSON string
        val json = gson.toJson(transactionList)
        editor.putString(TRANSACTIONS_KEY, json)
        editor.apply() // Use apply() for asynchronous saving
    }

    override fun onPause() {
        super.onPause()
        saveData()
    }





}