package com.example.spenttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.spenttracker.presentation.expenses.ExpensesScreen
import com.example.spenttracker.presentation.theme.SpentTrackerTheme

/**
 * Simple MainActivity - Just shows the expense form
 */
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            SpentTrackerTheme {
                ExpensesScreen()
            }
        }
    }
}