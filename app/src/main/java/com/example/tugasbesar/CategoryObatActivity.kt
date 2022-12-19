package com.example.tugasbesar

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.AuthFailureError
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.tugasbesar.adapters.CategoryObatAdapter
import com.example.tugasbesar.admin.AdminActivity
import com.example.tugasbesar.admin.AdminObatActivity
import com.example.tugasbesar.api.ObatApi
import com.example.tugasbesar.api.TransaksiApi
import com.example.tugasbesar.camera.CameraActivity
import com.example.tugasbesar.databinding.ActivityCategoryObatBinding
import com.example.tugasbesar.home.HomeActivity
import com.example.tugasbesar.map.MapActivity
import com.example.tugasbesar.models.Obat
import com.google.gson.Gson
import com.itextpdf.barcodes.BarcodeQRCode
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.property.HorizontalAlignment
import com.itextpdf.layout.property.TextAlignment
import kotlinx.android.synthetic.main.activity_home.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class CategoryObatActivity : AppCompatActivity() {
    private var srObat: SwipeRefreshLayout? = null
    private var adapter: CategoryObatAdapter? = null
    private var svObat: SearchView? = null
    private var layoutLoading: LinearLayout? = null
    private var queue: RequestQueue? = null

    companion object {
        const val LAUNCH_ADD_ACTIVITY = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        binding = ActivityCategoryObatBinding.inflate(layoutInflater)
//        val view: View = binding!!.root
//        setContentView(view)
        setContentView(R.layout.activity_category_obat)
        getSupportActionBar()?.hide()

        queue = Volley.newRequestQueue(this)
        layoutLoading = findViewById(R.id.layout_loading)
        srObat = findViewById(R.id.sr_categoryObat)
        svObat = findViewById(R.id.sv_categoryObat)

        srObat?.setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener { allObat() })
        svObat?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(s: String): Boolean {
                return false
            }

            override fun onQueryTextChange(s: String): Boolean {
                adapter!!.filter.filter(s)
                return false
            }
        })

        val rvProduk = findViewById<RecyclerView>(R.id.rv_categoryObat)
        adapter = CategoryObatAdapter(ArrayList(), this)
        rvProduk.layoutManager = LinearLayoutManager(this)
        rvProduk.adapter = adapter
        allObat()

        topAppBar.setNavigationOnClickListener {
            val back = Intent(this@CategoryObatActivity, HomeActivity::class.java)
            startActivity(back)
        }

        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.shopping -> {
                    val moveShopping = Intent(this@CategoryObatActivity, TransaksiActivity::class.java)
                    startActivity(moveShopping)
                    true
                }
                else -> false
            }
        }
    }

    private fun allObat() {
        srObat!!.isRefreshing = true
        val stringRequest: StringRequest = object :
            StringRequest(Method.GET, ObatApi.GET_ALL_URL, Response.Listener { response ->
                val gson = Gson()
                val jsonObject = JSONObject(response)
                val jsonArray = jsonObject.getJSONArray("data")
                var obat : Array<Obat> = gson.fromJson(jsonArray.toString(), Array<Obat>::class.java)

                adapter!!.setObatList(obat)
                adapter!!.filter.filter(svObat!!.query)
                srObat!!.isRefreshing = false

                if(!obat.isEmpty())
                    Toast.makeText(this@CategoryObatActivity, "Data Berhasil Diambil!", Toast.LENGTH_SHORT)
                        .show()
                else
                    Toast.makeText(this@CategoryObatActivity, "Data Kosong!", Toast.LENGTH_SHORT)
                        .show()
            }, Response.ErrorListener { error ->
                srObat!!.isRefreshing = false
                try {
                    val responseBody =
                        String(error.networkResponse.data, StandardCharsets.UTF_8)
                    val errors = JSONObject(responseBody)
                    Toast.makeText(
                        this@CategoryObatActivity,
                        errors.getString("message"),
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(this@CategoryObatActivity, e.message, Toast.LENGTH_SHORT).show()
                }
            }) {
            // Menambahkan header pada request
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Accept"] = "application/json"
                return headers
            }
        }
        queue!!.add(stringRequest)
    }

    fun create(obat: String, jenis: String, harga: String) {
        // Fungsi untuk menambah data mahasiswa.
        setLoading(true)

        val obat = Obat(
            obat,
            jenis,
            harga
        )
        val stringRequest: StringRequest =
            object : StringRequest(Method.POST, TransaksiApi.ADD_URL, Response.Listener { response ->
                val gson = Gson()
                var obat = gson.fromJson(response, Obat::class.java)

                if(obat != null)
                    Toast.makeText(this@CategoryObatActivity, "Data Berhasil Ditambahkan", Toast.LENGTH_SHORT).show()

                setLoading(false)
            }, Response.ErrorListener { error ->
                setLoading(false)
                try {
                    val responseBody = String(error.networkResponse.data, StandardCharsets.UTF_8)
                    val errors = JSONObject(responseBody)
                    Toast.makeText(
                        this@CategoryObatActivity,
                        errors.getString("message"),
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(this@CategoryObatActivity, e.message, Toast.LENGTH_SHORT).show()
                }
            }) {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val headers = HashMap<String, String>()
                    headers["Accept"] = "application/json"
                    return headers
                }

                @Throws(AuthFailureError::class)
                override fun getBody(): ByteArray {
                    val gson = Gson()
                    val requestBody = gson.toJson(obat)
                    return requestBody.toByteArray(StandardCharsets.UTF_8)
                }

                override fun getBodyContentType(): String {
                    return "application/json"
                }
            }
        // Menambahkan request ke request queue
        queue!!.add(stringRequest)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AdminObatActivity.LAUNCH_ADD_ACTIVITY && resultCode == RESULT_OK) allObat()
    }

    // Fungsi ini digunakan menampilkan layout loading
    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )
            layoutLoading!!.visibility = View.VISIBLE
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            layoutLoading!!.visibility = View.GONE
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Throws(
        FileNotFoundException::class
    )
    private fun createPDF(obat: String, jenis: String, harga: String) {
        //ini berguna untuk akses Writing ke Storage HP kalian dalam mode Download.
        //harus diketik jangan COPAS!!!!
        val  pdfpath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()
        val file = File(pdfpath, "tugaspbp.pdf")
        FileOutputStream(file)

        //inisialisasi pembuatan PDF
        val writer = PdfWriter(file)
        val pdfDocument = PdfDocument(writer)
        val document = Document(pdfDocument)
        pdfDocument.defaultPageSize = PageSize.A4
        document.setMargins(5f, 5f, 5f, 5f)
        @SuppressLint("UseCompatLoadingForDrawables") val d = getDrawable(R.drawable.transaction)

        //penambahan gambar pada Gambar atas
        val bitmap = (d as BitmapDrawable?)!!.bitmap
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val bitmapData = stream.toByteArray()
        val imageData = ImageDataFactory.create(bitmapData)
        val image = com.itextpdf.layout.element.Image(imageData)
        val namaPengguna = com.itextpdf.layout.element.Paragraph("Identitas Pembeli").setBold().setFontSize(24f)
            .setTextAlignment(TextAlignment.CENTER)
        val group = Paragraph(
            """
                        Berikut adalah
                        Nama Pembeli 2022/2023
                        """.trimIndent()).setTextAlignment(TextAlignment.CENTER).setFontSize(12f)
        val width = floatArrayOf(100f,100f)
        val table = Table(width)

        table.setHorizontalAlignment(HorizontalAlignment.CENTER)
        table.addCell(Cell().add(com.itextpdf.layout.element.Paragraph("Obat")))
        table.addCell(Cell().add(com.itextpdf.layout.element.Paragraph(obat)))
        table.addCell(Cell().add(com.itextpdf.layout.element.Paragraph("Jenis")))
        table.addCell(Cell().add(com.itextpdf.layout.element.Paragraph(jenis)))
        table.addCell(Cell().add(com.itextpdf.layout.element.Paragraph("Harga")))
        table.addCell(Cell().add(com.itextpdf.layout.element.Paragraph(harga)))
        table.addCell(Cell().add(com.itextpdf.layout.element.Paragraph("Tanggal Buat PDF")))
        val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        table.addCell(
            Cell().add(
                com.itextpdf.layout.element.Paragraph(
                    LocalDate.now().format(dateTimeFormatter)
                )
            ))
        table.addCell(Cell().add(com.itextpdf.layout.element.Paragraph("Pukul Pembuatan PDF")))
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        table.addCell(
            Cell().add(
                com.itextpdf.layout.element.Paragraph(
                    LocalTime.now().format(timeFormatter)
                )
            ))

        val barcodeQrCode = BarcodeQRCode(
            """
                $obat
                $jenis
                $harga
                ${LocalDate.now().format(dateTimeFormatter)}
                ${LocalTime.now().format(timeFormatter)}
                """.trimIndent()
        )
        val qrCodeObject = barcodeQrCode.createFormXObject(ColorConstants.BLACK, pdfDocument)
        val qrCodeImage = com.itextpdf.layout.element.Image(qrCodeObject).setWidth(80f).setHorizontalAlignment(
            HorizontalAlignment.CENTER)

        document.add(image)
        document.add(namaPengguna)
        document.add(group)
        document.add(table)
        document.add(qrCodeImage)

        document.close()
        Toast.makeText(this,"Pdf Created",Toast.LENGTH_SHORT).show()
    }
}