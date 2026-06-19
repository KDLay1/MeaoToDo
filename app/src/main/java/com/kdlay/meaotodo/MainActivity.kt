package com.kdlay.meaotodo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.kdlay.meaotodo.ui.MeaoTodoApp
import com.kdlay.meaotodo.ui.theme.MeaoTodoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MeaoTodoTheme {
                MeaoTodoApp()
            }
        }
    }
}
