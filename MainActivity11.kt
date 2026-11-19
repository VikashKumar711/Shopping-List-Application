
package com.example.myapplication

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Entity(tableName = "shopping_items")
data class Item(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val quantity: Int
)

@Dao
interface ShoppingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item)

    @Query("SELECT * FROM shopping_items ORDER BY id ASC")
    fun getAllItems(): Flow<List<Item>>

    @Query("SELECT COUNT(*) FROM shopping_items")
    suspend fun getItemCount(): Int
}

@Database(
    entities = [Item::class],
    version = 1,
    exportSchema = false
)
abstract class ShoppingDatabase : RoomDatabase() {

    abstract fun shoppingDao(): ShoppingDao

    companion object {
        @Volatile
        private var INSTANCE: ShoppingDatabase? = null

        fun getDatabase(context: Context): ShoppingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShoppingDatabase::class.java,
                    "shopping_database"
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}

class MainActivity11 : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val database = ShoppingDatabase.getDatabase(this)
        val shoppingDao = database.shoppingDao()

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    ShoppingListScreen(
                        dao = shoppingDao,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun ShoppingListScreen(
    dao: ShoppingDao,
    modifier: Modifier = Modifier
) {

    val items by dao.getAllItems().collectAsState(initial = emptyList())

    var itemName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Shopping List",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Maximum 3 items allowed",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = itemName,
            onValueChange = { itemName = it },
            label = { Text("Item Name") },
            placeholder = { Text("Enter item name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it },
            label = { Text("Quantity") },
            placeholder = { Text("Enter quantity") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {

                val name = itemName.trim()
                val qty = quantity.toIntOrNull()

                if (name.isEmpty()) {
                    message = "Please enter an item name"
                } else if (qty == null || qty <= 0) {
                    message = "Please enter a valid quantity"
                } else {

                    coroutineScope.launch {

                        val count = dao.getItemCount()

                        if (count >= 3) {
                            message = "Limit reached! Only 3 items are allowed."
                        } else {

                            dao.insertItem(
                                Item(
                                    name = name,
                                    quantity = qty
                                )
                            )

                            itemName = ""
                            quantity = ""
                            message = "Item added successfully!"
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Item")
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (message.isNotEmpty()) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Items: ${items.size}/3",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (items.isEmpty()) {

            Text(
                text = "Your shopping list is empty.",
                style = MaterialTheme.typography.bodyLarge
            )

        } else {

            items.forEach { item ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp
                    )
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Column {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                text = "Quantity: ${item.quantity}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Text(
                            text = "ID: ${item.id}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
