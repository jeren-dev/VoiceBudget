package com.example.voicebudget

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.*

class DBHelper(context: Context) :
    SQLiteOpenHelper(context, "expense.db", null, 6) {

    override fun onCreate(db: SQLiteDatabase) {
        // --- CORE TABLE: EXPENSES ---
        db.execSQL("""
            CREATE TABLE expenses(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                amount INTEGER,
                category TEXT,
                note TEXT,
                datetime TEXT
            )
        """.trimIndent())

        // --- CORE TABLE: LIMITS ---
        db.execSQL("""
            CREATE TABLE limits(
                period TEXT PRIMARY KEY, 
                amount INTEGER
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("DROP TABLE IF EXISTS expenses")
            onCreate(db)
        }
        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE IF NOT EXISTS limits(period TEXT PRIMARY KEY, amount INTEGER)")
        }
        if (oldVersion < 4) {
            // Add any new migrations here
        }
    }

    // --- NEW: BOOLEAN WRAPPER FOR SCANNER/VOICE ---
    fun addExpense(category: String, amount: Int, note: String = "Added via App"): Boolean {
        return try {
            val db = writableDatabase
            val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            // Store category with first letter capitalized for consistency
            val formattedCategory = category.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            val values = ContentValues().apply {
                put("amount", amount)
                put("category", formattedCategory)
                put("note", note)
                put("datetime", dateTime)
            }
            val result = db.insert("expenses", null, values)
            result != -1L
        } catch (e: Exception) {
            false
        }
    }

    // --- CORE LOGIC: INSERT ---
    fun insertExpense(amount: Int, category: String, note: String) {
        val db = writableDatabase
        val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date())

        // Store category with first letter capitalized for consistency
        val formattedCategory = category.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }

        val values = ContentValues().apply {
            put("amount", amount)
            put("category", formattedCategory)
            put("note", note)
            put("datetime", dateTime)
        }
        db.insert("expenses", null, values)
    }

    // --- CORE LOGIC: RETRIEVAL ---
    private fun getExpenses(whereClause: String? = null): List<ExpenseModel> {
        val list = mutableListOf<ExpenseModel>()
        val db = readableDatabase
        val query = buildString {
            append("SELECT * FROM expenses")
            if (!whereClause.isNullOrEmpty()) append(" WHERE $whereClause")
            append(" ORDER BY datetime DESC")
        }
        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    ExpenseModel(
                        amount = cursor.getInt(cursor.getColumnIndexOrThrow("amount")),
                        category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
                        note = cursor.getString(cursor.getColumnIndexOrThrow("note")),
                        datetime = cursor.getString(cursor.getColumnIndexOrThrow("datetime"))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    // --- FIXED: getCategoryTotalForToday (Case-Insensitive) ---
    fun getCategoryTotalForToday(category: String): Int {
        val db = readableDatabase
        val calendar = Calendar.getInstance()

        // Get today's date in yyyy-MM-dd format
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        val cursor = db.rawQuery(
            "SELECT SUM(amount) FROM expenses WHERE LOWER(category) = LOWER(?) AND date(datetime) = ?",
            arrayOf(category, todayDate)
        )
        var total = 0
        if (cursor.moveToFirst()) {
            total = cursor.getInt(0)
        }
        cursor.close()
        return total
    }

    // --- NEW METHOD: getCategoryTotalAllTime (Case-Insensitive) ---
    fun getCategoryTotalAllTime(category: String): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT IFNULL(SUM(amount),0) FROM expenses WHERE LOWER(category) = LOWER(?)",
            arrayOf(category)
        )
        var total = 0
        if (cursor.moveToFirst()) {
            total = cursor.getInt(0)
        }
        cursor.close()
        return total
    }

    // --- NEW METHOD: getAllUniqueCategories (Case-Insensitive, Returns Proper Case) ---
    fun getAllUniqueCategories(): List<String> {
        val categories = mutableListOf<String>()
        val db = readableDatabase
        // Get unique categories, case-insensitive, return the most common capitalization
        val cursor = db.rawQuery("""
            SELECT category, COUNT(*) as cnt 
            FROM expenses 
            GROUP BY LOWER(category) 
            ORDER BY cnt DESC, category COLLATE NOCASE
        """, null)
        if (cursor.moveToFirst()) {
            do {
                val category = cursor.getString(cursor.getColumnIndexOrThrow("category"))
                // Capitalize first letter for display
                val displayCategory = category.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
                categories.add(displayCategory)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return categories
    }

    // --- NEW METHOD: getCategorySummaryAllTime (Case-Insensitive Grouping) ---
    fun getCategorySummaryAllTime(): Map<String, Int> {
        val summary = mutableMapOf<String, Int>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT category, SUM(amount) as total 
            FROM expenses 
            GROUP BY LOWER(category) 
            ORDER BY total DESC
            """,
            null
        )
        if (cursor.moveToFirst()) {
            do {
                val category = cursor.getString(cursor.getColumnIndexOrThrow("category"))
                val total = cursor.getInt(cursor.getColumnIndexOrThrow("total"))
                if (total > 0) {
                    val displayCategory = category.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    }
                    // Merge totals for same display category
                    summary[displayCategory] = summary.getOrDefault(displayCategory, 0) + total
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        return summary
    }

    fun getTodayExpenses() = getExpenses("date(datetime) = date('now')")
    fun getWeekExpenses() = getExpenses("datetime >= datetime('now','-7 days')")
    fun getMonthExpenses() = getExpenses("strftime('%m', datetime) = strftime('%m', 'now')")
    fun getYearExpenses() = getExpenses("strftime('%Y', datetime) = strftime('%Y', 'now')")

    // --- CORE LOGIC: TOTALS ---
    private fun getTotal(whereClause: String? = null): Int {
        val db = readableDatabase
        val query = buildString {
            append("SELECT IFNULL(SUM(amount),0) FROM expenses")
            if (!whereClause.isNullOrEmpty()) append(" WHERE $whereClause")
        }
        val cursor = db.rawQuery(query, null)
        val total = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return total
    }

    fun getTodayTotal() = getTotal("date(datetime) = date('now')")
    fun getWeekTotal() = getTotal("datetime >= datetime('now','-7 days')")
    fun getMonthTotal() = getTotal("strftime('%m', datetime) = strftime('%m', 'now')")
    fun getYearTotal() = getTotal("strftime('%Y', datetime) = strftime('%Y', 'now')")

    // --- NEW METHODS FOR CATEGORY BREAKDOWN (Case-Insensitive) ---
    fun getCategoryTotalForMonth(category: String): Int {
        val db = readableDatabase
        val query = """
            SELECT IFNULL(SUM(amount),0) FROM expenses 
            WHERE LOWER(category) = LOWER(?) AND strftime('%m', datetime) = strftime('%m', 'now')
            AND strftime('%Y', datetime) = strftime('%Y', 'now')
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(category))
        val total = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return total
    }

    // --- NEW METHOD: getCategoryTotalForWeek (Case-Insensitive) ---
    fun getCategoryTotalForWeek(category: String): Int {
        val db = readableDatabase
        val query = """
            SELECT IFNULL(SUM(amount),0) FROM expenses 
            WHERE LOWER(category) = LOWER(?) AND datetime >= datetime('now','-7 days')
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(category))
        val total = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return total
    }

    // --- NEW METHOD: getCategoryTotalForYear (Case-Insensitive) ---
    fun getCategoryTotalForYear(category: String): Int {
        val db = readableDatabase
        val query = """
            SELECT IFNULL(SUM(amount),0) FROM expenses 
            WHERE LOWER(category) = LOWER(?) AND strftime('%Y', datetime) = strftime('%Y', 'now')
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(category))
        val total = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return total
    }

    // --- NEW METHOD FOR DATE-SPECIFIC TOTAL (for Trade Graph) ---
    fun getTotalForDate(date: Date): Int {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = sdf.format(date)
        val db = readableDatabase
        val query = """
            SELECT IFNULL(SUM(amount),0) FROM expenses 
            WHERE date(datetime) = ?
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(dateString))
        val total = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return total
    }

    // --- NEW METHOD FOR RECENT TRANSACTIONS ---
    data class RecentTransaction(val category: String, val amount: Int, val date: String)

    fun getRecentTransactions(limit: Int): List<RecentTransaction> {
        val list = mutableListOf<RecentTransaction>()
        val db = readableDatabase
        val query = """
            SELECT category, amount, datetime FROM expenses 
            ORDER BY datetime DESC 
            LIMIT ?
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(limit.toString()))
        if (cursor.moveToFirst()) {
            do {
                val dateTime = cursor.getString(cursor.getColumnIndexOrThrow("datetime"))
                val displayDate = if (dateTime.length >= 10) dateTime.substring(0, 10) else dateTime
                val category = cursor.getString(cursor.getColumnIndexOrThrow("category"))
                val displayCategory = category.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
                list.add(
                    RecentTransaction(
                        category = displayCategory,
                        amount = cursor.getInt(cursor.getColumnIndexOrThrow("amount")),
                        date = displayDate
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    // --- NEW METHOD FOR GETTING ALL EXPENSES (for Analytics) ---
    fun getAllExpenses(): List<ExpenseModel> {
        return getExpenses()
    }

    // --- NEW METHOD FOR GETTING EXPENSES BY CATEGORY (Case-Insensitive) ---
    fun getExpensesByCategory(category: String): List<ExpenseModel> {
        return getExpenses("LOWER(category) = LOWER('$category')")
    }

    // --- NEW METHOD FOR GETTING EXPENSES BY DATE RANGE ---
    fun getExpensesByDateRange(startDate: String, endDate: String): List<ExpenseModel> {
        return getExpenses("date(datetime) BETWEEN '$startDate' AND '$endDate'")
    }

    // --- NEW METHOD FOR CATEGORY SUMMARY FOR CURRENT MONTH (Case-Insensitive Grouping) ---
    fun getCategorySummaryForMonth(): Map<String, Int> {
        val summary = mutableMapOf<String, Int>()
        val db = readableDatabase
        val query = """
            SELECT category, SUM(amount) as total 
            FROM expenses 
            WHERE strftime('%m', datetime) = strftime('%m', 'now')
            AND strftime('%Y', datetime) = strftime('%Y', 'now')
            GROUP BY LOWER(category)
            ORDER BY total DESC
        """.trimIndent()
        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            do {
                val category = cursor.getString(cursor.getColumnIndexOrThrow("category"))
                val total = cursor.getInt(cursor.getColumnIndexOrThrow("total"))
                if (total > 0) {
                    val displayCategory = category.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                    }
                    summary[displayCategory] = summary.getOrDefault(displayCategory, 0) + total
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        return summary
    }

    // --- NEW METHOD FOR TOTAL SPENDING BY DAY (for Trade Graph enhancement) ---
    fun getLastNDaysTotal(days: Int): List<Pair<String, Int>> {
        val result = mutableListOf<Pair<String, Int>>()
        val db = readableDatabase
        val query = """
            SELECT date(datetime) as day, SUM(amount) as total 
            FROM expenses 
            WHERE datetime >= date('now', '-$days days')
            GROUP BY date(datetime)
            ORDER BY day ASC
        """.trimIndent()
        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            do {
                val day = cursor.getString(cursor.getColumnIndexOrThrow("day"))
                val total = cursor.getInt(cursor.getColumnIndexOrThrow("total"))
                result.add(Pair(day, total))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return result
    }

    // --- CORE LOGIC: LIMITS ---
    fun saveLimit(period: String, amount: Int) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("period", period)
            put("amount", amount)
        }
        db.insertWithOnConflict("limits", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getLimit(period: String): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT amount FROM limits WHERE period = ?", arrayOf(period))
        val limit = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return limit
    }

    fun deleteLimit(period: String) {
        val db = writableDatabase
        db.delete("limits", "period = ?", arrayOf(period))
    }

    // --- NEW METHOD TO GET ALL LIMITS ---
    fun getAllLimits(): Map<String, Int> {
        val limits = mutableMapOf<String, Int>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT period, amount FROM limits", null)
        if (cursor.moveToFirst()) {
            do {
                val period = cursor.getString(cursor.getColumnIndexOrThrow("period"))
                val amount = cursor.getInt(cursor.getColumnIndexOrThrow("amount"))
                limits[period] = amount
            } while (cursor.moveToNext())
        }
        cursor.close()
        return limits
    }

    // --- HELPER METHOD TO CHECK IF LIMIT EXISTS ---
    fun limitExists(period: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT 1 FROM limits WHERE period = ?", arrayOf(period))
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }

    // --- DELETE ALL EXPENSES (for testing/reset) ---
    fun deleteAllExpenses(): Int {
        val db = writableDatabase
        return db.delete("expenses", null, null)
    }

    // --- DELETE EXPENSES BY CATEGORY (Case-Insensitive) ---
    fun deleteExpensesByCategory(category: String): Int {
        val db = writableDatabase
        return db.delete("expenses", "LOWER(category) = LOWER(?)", arrayOf(category))
    }

    // --- GET TOTAL COUNT OF EXPENSES ---
    fun getExpensesCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM expenses", null)
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    // --- GET AVERAGE SPENDING PER DAY (current month) ---
    fun getAverageDailySpending(): Float {
        val monthTotal = getMonthTotal()
        val calendar = Calendar.getInstance()
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        return if (daysInMonth > 0) monthTotal.toFloat() / daysInMonth else 0f
    }

    // --- GET HIGHEST SPENDING CATEGORY FOR CURRENT MONTH (DYNAMIC, Case-Insensitive) ---
    fun getHighestSpendingCategory(): String {
        var maxCategory = "None"
        var maxAmount = 0
        val db = readableDatabase
        val query = """
            SELECT category, SUM(amount) as total 
            FROM expenses 
            WHERE strftime('%m', datetime) = strftime('%m', 'now')
            AND strftime('%Y', datetime) = strftime('%Y', 'now')
            GROUP BY LOWER(category)
            ORDER BY total DESC 
            LIMIT 1
        """.trimIndent()
        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            maxCategory = cursor.getString(cursor.getColumnIndexOrThrow("category"))
            maxAmount = cursor.getInt(cursor.getColumnIndexOrThrow("total"))
        }
        cursor.close()
        return maxCategory.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }

    // --- GET TOTAL SPENDING FOR CUSTOM DATE RANGE ---
    fun getTotalForDateRange(startDate: String, endDate: String): Int {
        val db = readableDatabase
        val query = """
            SELECT IFNULL(SUM(amount),0) FROM expenses 
            WHERE date(datetime) BETWEEN ? AND ?
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(startDate, endDate))
        val total = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return total
    }

    // --- SEARCH EXPENSES BY NOTE OR CATEGORY (Case-Insensitive) ---
    fun searchExpenses(searchTerm: String): List<ExpenseModel> {
        return getExpenses("LOWER(category) LIKE LOWER('%$searchTerm%') OR LOWER(note) LIKE LOWER('%$searchTerm%')")
    }

    // --- GET MONTHLY TREND DATA (last 6 months) ---
    fun getMonthlyTrendData(): List<Pair<String, Int>> {
        val result = mutableListOf<Pair<String, Int>>()
        val db = readableDatabase
        val query = """
            SELECT strftime('%Y-%m', datetime) as month, SUM(amount) as total 
            FROM expenses 
            WHERE datetime >= date('now', '-6 months')
            GROUP BY strftime('%Y-%m', datetime)
            ORDER BY month ASC
        """.trimIndent()
        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            do {
                val month = cursor.getString(cursor.getColumnIndexOrThrow("month"))
                val total = cursor.getInt(cursor.getColumnIndexOrThrow("total"))
                result.add(Pair(month, total))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return result
    }
}