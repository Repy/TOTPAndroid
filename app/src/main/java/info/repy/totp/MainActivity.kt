package info.repy.totp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.repy.totp.ui.theme.TOTPAndroidTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jboss.aerogear.security.otp.Totp
import java.util.Date

private fun OTP(key: String): String {
    try {
        val totp = Totp(key)
        return totp.now()
    } catch (e: Exception) {
        return ""
    }
}


class MainActivity : ComponentActivity() {
    private var dao: InventoryDatabase? = null
    private var itemList = mutableStateListOf<Item>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        this.setContent {
            TOTPAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Page(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
        dao = InventoryDatabase.getDatabase(this.application)
        loadData()
    }

    private fun loadData() {
        val dao = this.dao
        if (dao != null) {
            val data = dao.itemDao().getAllItems()
            CoroutineScope(Dispatchers.Main).launch {
                data.collect { d ->
                    itemList.clear()
                    itemList.addAll(d)
                }
            }
        }
    }

    private fun insertData(name: String, pass: String) {
        val dao = this.dao
        if (dao != null) {
            CoroutineScope(Dispatchers.Main).launch {
                dao.itemDao().insert(Item(0, name, pass))
                loadData()
            }
        }
    }

    fun deleteData(i: Item) {
        val dao = this.dao
        if (dao != null) {
            CoroutineScope(Dispatchers.Main).launch {
                dao.itemDao().delete(i)
                loadData()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Preview(showSystemUi = true)
    @Composable
    fun Page(modifier: Modifier = Modifier) {
        val time = remember { mutableLongStateOf(Date().time / 1000L / 30L) }
        LaunchedEffect(time.longValue) {
            while (true) {
                delay(500L)
                time.longValue = Date().time / 30000L
            }
        }

        val scrollState =  rememberScrollState()
        val openAlertDialog = remember { mutableStateOf(false) }
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        Text("TOTP")
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    openAlertDialog.value = true
                }) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Add"
                    )
                }
            },
            modifier = modifier
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(innerPadding),
            ) {
                itemList.forEach { i ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                    ) {
                        Text(
                            text = i.name,
                            fontSize = 20.sp,
                        )
                        Text(
                            text = OTP(i.pass),
                            fontSize = 30.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
        if (openAlertDialog.value) AddDialog(openAlertDialog)
    }

    @Composable
    fun AddDialog(openAlertDialog: MutableState<Boolean>) {
        val name = remember { mutableStateOf("") }
        val pass = remember { mutableStateOf("") }
        AlertDialog(
            title = { Text("追加") },
            onDismissRequest = { openAlertDialog.value = false },
            confirmButton = {
                Button(
                    onClick = {
                        openAlertDialog.value = false
                        insertData(name.value, pass.value)
                    },
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text("保存")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start,
                ) {
                    OutlinedTextField(
                        value = name.value,
                        onValueChange = { d ->
                            name.value = d
                        },
                        label = { Text("名前") },
                    )
                    OutlinedTextField(
                        value = pass.value,
                        onValueChange = { d ->
                            pass.value = d
                        },
                        label = { Text("キー") },
                    )
                }
            },
        )
    }
}
