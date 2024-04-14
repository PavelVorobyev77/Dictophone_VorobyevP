package com.example.dictophone_vorobyevp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Delete
import androidx.room.Room
import androidx.room.RoomDatabase
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

class GalleryActivity : AppCompatActivity(), OnItemClickListener {

    private lateinit var recycleview: RecyclerView
    private lateinit var records: ArrayList<AudioRecord>
    private lateinit var mAdapter: Adapter
    private lateinit var db: AppDatabase
    private lateinit var searchInput: TextInputEditText
    private lateinit var toolbar: MaterialToolbar

    private lateinit var editBar: View
    private lateinit var btnClose: ImageButton
    private lateinit var btnSelectAll: ImageButton
    //private lateinit var btnShare: ImageButton
    private lateinit var bottomSheet: LinearLayout
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var tvDelete: TextView
    private lateinit var tvRename: TextView
    private lateinit var btnDelete: ImageButton
    private lateinit var btnRename: ImageButton

    private enum class SortType {
        ALPHABET,
        DURATION
    }

    private var currentSortType: SortType? = null

    private var allChecked = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        editBar = findViewById(R.id.editBar)
        recycleview = findViewById(R.id.recycleview)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        //btnShare = findViewById(R.id.btnShare)
        btnDelete = findViewById(R.id.btnDelete)
        btnRename = findViewById(R.id.btnEdit)
        tvDelete = findViewById(R.id.tvDelete)
        tvRename = findViewById(R.id.tvEdit)

        btnClose = findViewById(R.id.btnClose)
        btnSelectAll = findViewById(R.id.btnSelectAll)

        bottomSheet = findViewById(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        records = ArrayList()

        db = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "audioRecords"
        ).build()

        mAdapter = Adapter(records, this)

        recycleview.apply{
            adapter = mAdapter
            layoutManager = LinearLayoutManager(context)
        }

        fetchAll()

        searchInput = findViewById(R.id.search_input)
        searchInput.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                var query = p0.toString()
                searchDatabase(query)
            }

            override fun afterTextChanged(p0: Editable?) {
            }

        })

        btnClose.setOnClickListener{
            leaveEditMode()

        }

        btnSelectAll.setOnClickListener{
            allChecked = !allChecked
            records.map{it.isChecked = allChecked}
            mAdapter.notifyDataSetChanged()

            if(allChecked){
                disableRename()
                enableDelete()
            }else{
                disableDelete()
                disableRename()
            }

        }

        btnDelete.setOnClickListener{
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Удалить запись?")
            val nbRecords = records.count{it.isChecked}
            builder.setMessage("Ты уверен, что хочешь удалить $nbRecords запись(си) ?")

            builder.setPositiveButton("Удалить"){_,_->
                val toDelete = records.filter{it.isChecked}.toTypedArray()
                GlobalScope.launch {
                    db.audioRecordDao().delete(toDelete)
                    runOnUiThread{
                        records.removeAll(toDelete)
                        mAdapter.notifyDataSetChanged()
                        leaveEditMode()
                    }
                }

            }

            builder.setNegativeButton("Отмена"){_,_->
                //...

            }
            val dialog = builder.create()
            dialog.show()

        }

        btnRename.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val dialogView = this.layoutInflater.inflate(R.layout.rename_layout, null)
            builder.setView(dialogView)
            val dialog = builder.create()

            val record = records.filter{it.isChecked}.get(0)
            val textInput = dialogView.findViewById<TextInputEditText>(R.id.filenameInput)
            textInput.setText(record.filename)

            dialogView.findViewById<Button>(R.id.btnSave).setOnClickListener{
                val input = textInput.text.toString()
                if(input.isEmpty()){
                    Toast.makeText(this, "Назовите запись", Toast.LENGTH_LONG).show()
                }else{
                    record.filename = input
                    GlobalScope.launch {
                        db.audioRecordDao().update(record)
                        runOnUiThread{
                            mAdapter.notifyItemChanged(records.indexOf(record))
                            dialog.dismiss()
                            leaveEditMode()
                        }
                    }
                }
            }

            dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener{
                dialog.dismiss()
            }

            dialog.show()

        }

        //проклятая сортировка!!!!
        val spinnerSortOptions = findViewById<Spinner>(R.id.spinnerSortOptions)
        val adapter = ArrayAdapter.createFromResource(this, R.array.sort_options, android.R.layout.simple_spinner_item).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerSortOptions.adapter = adapter

        spinnerSortOptions.setSelection(0) // Выбор "Не сортировать" при старте
        fetchRecords() // Первичная загрузка записей без сортировки
        spinnerSortOptions.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentSortType = when (position) {
                    0 -> null
                    1 -> SortType.ALPHABET
                    2 -> SortType.DURATION
                    else -> null
                }
                if (currentSortType != null) {
                    fetchRecords()
                } else {
                    fetchRecords()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                currentSortType = null
                fetchRecords()
            }
        }


    }

    private fun leaveEditMode(){
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        editBar.visibility = View.GONE

        // Задержка перед скрытием bottom sheet после свертывания
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        val handler = Handler()
        handler.postDelayed({
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }, 250) // Время задержки

        records.map { it.isChecked = false}
        mAdapter.setEditMode(false)
    }

    private fun disableRename() {
        btnRename.isClickable = false
        btnRename.backgroundTintList = ResourcesCompat.getColorStateList(resources, R.color.grayDarkDisabled, theme)
        tvRename.setTextColor(ResourcesCompat.getColorStateList(resources, R.color.grayDarkDisabled, theme))
    }

    private fun disableDelete() {
        btnDelete.isClickable = false
        btnDelete.backgroundTintList = ResourcesCompat.getColorStateList(resources, R.color.grayDarkDisabled, theme)
        tvDelete.setTextColor(ResourcesCompat.getColorStateList(resources, R.color.grayDarkDisabled, theme))
    }

    private fun enableRename() {
        btnRename.isClickable = true
        btnRename.backgroundTintList = ResourcesCompat.getColorStateList(resources, R.color.grayDark, theme)
        tvRename.setTextColor(ResourcesCompat.getColorStateList(resources, R.color.grayDark, theme))
    }

    private fun enableDelete() {
        btnDelete.isClickable = true
        btnDelete.backgroundTintList = ResourcesCompat.getColorStateList(resources, R.color.grayDark, theme)
        tvDelete.setTextColor(ResourcesCompat.getColorStateList(resources, R.color.grayDark, theme))
    }

    private fun searchDatabase(query: String){
        GlobalScope.launch {
            records.clear()
            var queryResult = db.audioRecordDao().searchDatabase("%$query%")
            records.addAll(queryResult)

            runOnUiThread{
                mAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun fetchAll(){
        GlobalScope.launch {
            records.clear()
            var queryResult = db.audioRecordDao().getAll()
            records.addAll(queryResult)

            mAdapter.notifyDataSetChanged()
        }
    }

    private fun fetchRecords() {
        GlobalScope.launch {
            records.clear()
            when (currentSortType) {
                SortType.ALPHABET -> {
                    val queryResult = db.audioRecordDao().getAllSortedByAlphabet()
                    records.addAll(queryResult)
                }
                SortType.DURATION -> {
                    val queryResult = db.audioRecordDao().getAllSortedByDuration()
                    records.addAll(queryResult)
                }
                null -> {
                    val queryResult = db.audioRecordDao().getAll()
                    records.addAll(queryResult)
                }
            }

            runOnUiThread {
                mAdapter.notifyDataSetChanged()
            }
        }
    }


    override fun onItemClickListener(position: Int) {
        val audioRecord = records[position]
        if (mAdapter.isEditMode()) {
            records[position].isChecked = !records[position].isChecked
            mAdapter.notifyItemChanged(position)

            var nbSelected = records.count{it.isChecked}
            when(nbSelected){
                0 -> {
                    disableRename()
                    disableDelete()
                }
                1 -> {
                    enableRename()
                    enableDelete()
                }
                else -> {
                    disableRename()
                    enableDelete()
                }
            }

        } else {
            val intent = Intent(this, AudioPlayerActivity::class.java)
            intent.putExtra("filepath", audioRecord.filePath)
            intent.putExtra("filename", audioRecord.filename)
            startActivity(intent)
        }
    }

    private fun sortRecordsByDuration() {
        records.sortBy { it.duration}
        mAdapter.notifyDataSetChanged()
    }

    private fun sortRecordsByAlphabet() {
        records.sortBy { it.filename }
        mAdapter.notifyDataSetChanged()
    }

    override fun onItemLongClickListener(position: Int) {
        mAdapter.setEditMode(true)
        records[position].isChecked = !records[position].isChecked
        mAdapter.notifyItemChanged(position)

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        if (mAdapter.isEditMode() && editBar.visibility == View.GONE){
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            supportActionBar?.setDisplayShowHomeEnabled(false)

            editBar.visibility = View.VISIBLE

            enableDelete()
            enableRename()
        }
    }

}
