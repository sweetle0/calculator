package com.example.calculator

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import java.math.BigDecimal
import java.math.RoundingMode

class CalculatorViewModel : ViewModel() {
    var history by mutableStateOf("")   // предыдущее выражение: "5 + 10 ×"
        private set
    var operation by mutableStateOf("") // текущая операция
        private set
    var current by mutableStateOf("0")  // текущее число
        private set

    private val tokens = mutableListOf<String>() // ["5", "+", "10", "×"]
    private var newInput = false                 // следующая цифра начинает новое число

    fun onDigit(d: String) {
        current = if (newInput || current == "0") d else current + d
        newInput = false
    }

    fun onDot() {
        if (newInput) { current = "0."; newInput = false }
        else if (!current.contains(".")) current += "."
    }

    fun onOperator(op: String) {
        if (current == "Ошибка") return
        if (newInput && tokens.isNotEmpty()) tokens[tokens.lastIndex] = op // замена оператора
        else { tokens.add(current); tokens.add(op) }
        operation = op
        history = tokens.joinToString(" ")
        current = "0"
        newInput = true
    }

    fun onEquals() {
        if (tokens.isEmpty() || newInput) return
        val all = tokens + current
        val result = calculate(all)
        history = all.joinToString(" ") + " ="
        operation = ""
        current = result?.let { format(it) } ?: "Ошибка"
        tokens.clear()
        newInput = true
    }

    fun onClear() {
        tokens.clear()
        history = ""; operation = ""; current = "0"; newInput = false
    }

    // Сначала × и ÷, затем + и −
    private fun calculate(t: List<String>): Double? {
        val nums = mutableListOf(t[0].toDouble())
        val ops = mutableListOf<String>()
        for (i in 1 until t.size step 2) {
            val n = t[i + 1].toDouble()
            when (t[i]) {
                "×" -> nums[nums.lastIndex] *= n
                "÷" -> { if (n == 0.0) return null; nums[nums.lastIndex] /= n }
                else -> { ops.add(t[i]); nums.add(n) }
            }
        }
        var res = nums[0]
        ops.forEachIndexed { i, op -> res = if (op == "+") res + nums[i + 1] else res - nums[i + 1] }
        return res
    }

    private fun format(v: Double): String =
        BigDecimal(v.toString()).setScale(10, RoundingMode.HALF_UP)
            .stripTrailingZeros().toPlainString()
}

class MainActivity : ComponentActivity() {
    private val vm: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { CalculatorScreen(vm) } }
    }
}

@Composable
fun CalculatorScreen(vm: CalculatorViewModel) {
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Surface(Modifier.fillMaxSize()) {
        // Display всегда сверху, Keyboard под ним; в альбомной ориентации шрифты компактнее
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(8.dp)) {
            Display(vm, landscape, Modifier.weight(1f).fillMaxWidth())
            Keyboard(vm, landscape, Modifier.weight(2f).fillMaxWidth())
        }
    }
}

@Composable
fun Display(vm: CalculatorViewModel, compact: Boolean, modifier: Modifier) {
    Column(
        modifier.padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End
    ) {
        // 1. Предыдущее выражение - маленький серый шрифт
        Text(vm.history, fontSize = if (compact) 14.sp else 18.sp, color = Color.Gray, maxLines = 1)
        // 2. Текущая операция
        Text(vm.operation, fontSize = if (compact) 18.sp else 28.sp, maxLines = 1)
        // 3. Текущее число - крупный шрифт
        Text(vm.current, fontSize = if (compact) 32.sp else 48.sp, maxLines = 1)
    }
}

@Composable
fun Keyboard(vm: CalculatorViewModel, compact: Boolean, modifier: Modifier) {
    val rows = listOf(
        listOf("AC", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "−"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "=")
    )
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (row in rows) {
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (key in row) {
                    Button(
                        onClick = {
                            when (key) {
                                "AC" -> vm.onClear()
                                "." -> vm.onDot()
                                "=" -> vm.onEquals()
                                "÷", "×", "−", "+" -> vm.onOperator(key)
                                else -> vm.onDigit(key)
                            }
                        },
                        modifier = Modifier
                            .weight(if (key == "AC") 3f else if (key == "0") 2f else 1f)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text(key, fontSize = if (compact) 18.sp else 24.sp) }
                }
            }
        }
    }
}