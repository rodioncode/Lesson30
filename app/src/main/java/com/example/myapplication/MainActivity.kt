package com.example.myapplication

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.databinding.ItemBookBinding
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll() // или .detectDiskReads(), .detectNetwork() и т.д.
                    .penaltyLog() // выводит предупреждения в Logcat
                    .penaltyDialog() // всплывающее окно (для Activity)
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll() // проверяет утечки Activity, неправильные вызовы и т.д.
                    .penaltyLog()
                    .build()
            )
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        binding.loadButton.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val books =
                    BookApi.service.getBooks("android")
                withContext(Dispatchers.Main) {
                    binding.recyclerView.adapter = BookAdapter(books.docs) {
                        startActivity(Intent(this@MainActivity, DetailsActivity::class.java))
                    }
                }
            }
                /*var sum = 0L
                for (i in 1..1_000_000_000) { // долгий цикл
                    sum += i
                }
                Toast.makeText(this, "Sum $sum", Toast.LENGTH_SHORT).show()*/
        }

        binding.syncButton.setOnClickListener {
            val workRequest = OneTimeWorkRequestBuilder<BookSyncWorker>().build()
            WorkManager.getInstance(this).enqueue(workRequest)
            startActivity(Intent(this, DetailsActivity2::class.java))
        }

        Handler(Looper.getMainLooper()).postDelayed({
            Thread.sleep(8000)
        }, 2000)
        handler.postDelayed(runnable, 60000)
    }
}

object ViewHolder {
    val Views = mutableListOf<View>()
    fun add(view: View) {
        Views.add(view)
    }
    fun remove(view: View) {
        Views.remove(view)
    }
}

private val handler = Handler(Looper.getMainLooper())
private val runnable = Runnable {
    println("Delayed task")
}

class DetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Thread.sleep(3000)
        resources
        LeakySingleton.activity =this
        lifecycleScope.launch(Dispatchers.IO) {
            delay(3000L)
        }

        setContentView(R.layout.activity_details)

    }
}
class DetailsActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Thread.sleep(3000)
        LeakySingleton.getText()
        lifecycleScope.launch(Dispatchers.IO) {
            delay(3000L)
        }
        setContentView(R.layout.activity_details2)
    }
}

object LeakySingleton {
    var activity: Activity? = null
    fun getText() = activity?.getString(R.string.app_name)
}

data class BookResponse(val docs: List<Book>)
data class Book(val title: String)

interface BookApiService {
    @GET("search.json")
    suspend fun getBooks(@Query("q") query: String): BookResponse
}

object BookApi {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val service: BookApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://openlibrary.org/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(BookApiService::class.java)
    }
}

class BookAdapter(
    private val books: List<Book>,
    private val onClick: (Book) -> Unit,
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    inner class BookViewHolder(private val binding: ItemBookBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(book: Book) {
            binding.titleText.text = book.title
            binding.root.setOnClickListener { onClick(book) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(books[position])
    }

    override fun getItemCount() = books.size
}

class BookSyncWorker(appContext: Context, params: WorkerParameters) : Worker(appContext, params) {
    override fun doWork(): Result {
        // Эмуляция тяжёлой фоновой задачи
        Thread.sleep(5000)
        return Result.success()
    }
}



