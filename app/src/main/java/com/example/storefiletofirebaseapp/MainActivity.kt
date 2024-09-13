package com.example.storefiletofirebaseapp

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.storefiletofirebaseapp.databinding.ActivityMainBinding
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var pdfFiles: Array<String>
    private var mSelectedItem: Int = -1
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var storageReference: StorageReference
    private lateinit var fileName: String
    private lateinit var fileUri: Uri
    private var tempFile: File? = null
    private var startTime: Long = 0L
    private var endTime: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storageReference = FirebaseStorage.getInstance().reference

        getFilesFromAssets()
        setAdapter()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.pdfListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, view, position, _ ->
                val selectedPdf = pdfFiles[position]
                mSelectedItem = position
                fileName = selectedPdf
                fileUri = Uri.fromFile(File("assets/$fileName"))

                tempFile = copyFileFromAssetsToCache(fileName)

                Log.d("TAG", "File name: $fileName $fileUri")
                if (mSelectedItem == position) {
                    view.setBackgroundColor(Color.GRAY)
                } else {
                    view.setBackgroundColor(Color.WHITE)
                }

                adapter.notifyDataSetChanged()
            }

        binding.btnUploadFile.setOnClickListener {
            tempFile?.let { uploadFileToFirebase(it) }
        }

    }

    private fun setAdapter() {
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, pdfFiles)
        binding.pdfListView.adapter = adapter

        val selectedPdf = pdfFiles[0]
        mSelectedItem = 0
        fileName = selectedPdf
        fileUri = Uri.fromFile(File("ass    ets/$fileName"))

        tempFile = copyFileFromAssetsToCache(fileName)

        Log.d("TAG", "File name: $fileName $fileUri")


        adapter.notifyDataSetChanged()
    }

    private fun getFilesFromAssets() {
        try {
            pdfFiles = assets.list("")?.filter { it.endsWith(".pdf") }?.toTypedArray() ?: arrayOf()
        } catch (e: IOException) {
            e.printStackTrace()
            pdfFiles = arrayOf()
        }
    }


    @SuppressLint("SetTextI18n")
    private fun uploadFileToFirebase(file: File) {
        startTime = System.currentTimeMillis()

        // Create a reference to the location in Firebase Storage where the file will be uploaded
        val fileRef = storageReference.child("uploads/${file.name}")

        // Get the file's URI
        val fileUri: Uri = Uri.fromFile(file)

        // Start uploading the file
        fileRef.putFile(fileUri)
            .addOnSuccessListener {
                // Handle successful upload
                fileRef.downloadUrl.addOnSuccessListener { uri ->
                    Toast.makeText(
                        this,
                        "File uploaded successfully. Download URL: $uri",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener { exception ->
                // Handle unsuccessful upload
                Toast.makeText(this, "Upload failed: ${exception.message}", Toast.LENGTH_LONG)
                    .show()
            }
            .addOnProgressListener { taskSnapshot ->
                // Track upload progress
                val progress: Int =
                    (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                binding.txtProgress.visibility = View.VISIBLE
                binding.txtProgress.text = "Upload is $progress done"
                if (progress == 100) {
                    endTime = System.currentTimeMillis()
                    val difference: Long = endTime / 1000 - startTime / 1000
                    Log.d("TAG", "uploadFileToFirebase: $startTime $endTime $difference")
                    binding.txtProgress.visibility = View.GONE
                    binding.timer.text = "File Upload time is $difference seconds"
                }
            }
    }

    private fun copyFileFromAssetsToCache(assetFileName: String): File? {
        try {
            // Open the asset file as an InputStream
            val assetManager = assets
            val inputStream: InputStream = assetManager.open(assetFileName)

            // Create a temporary file in the cache directory
            val tempFile = File(cacheDir, assetFileName)

            // Write the asset file's contents to the temp file
            val outputStream = FileOutputStream(tempFile)
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            // Return the temporary file
            return tempFile
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error copying file from assets", Toast.LENGTH_LONG).show()
        }

        return null
    }
}