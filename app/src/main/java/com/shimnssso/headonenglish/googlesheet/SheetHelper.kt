package com.shimnssso.headonenglish.googlesheet

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.google.api.services.drive.Drive
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.Spreadsheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException

object SheetHelper {
    private var drive: Drive? = null
    private var sheets: Sheets? = null

    fun init(drive: Drive, sheets: Sheets) {
        this.drive = drive
        this.sheets = sheets
    }

    private fun isInitialized(): Boolean {
        return (drive != null) && (sheets != null)
    }

    fun getFilePickerIntent(): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        // intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.type = "application/vnd.google-apps.spreadsheet"

        // Optionally, specify a URI for the directory that should be opened in
        // the system file picker when it loads.
        // intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
        return intent
    }

    fun requestSheetId(contentResolver: ContentResolver, uri: Uri?, callback: (String?) -> Unit) {
        if (!isInitialized()) {
            throw IOException("SheetHelper has not been initialized yet!!")
        }

        CoroutineScope(Dispatchers.IO).launch {
            var fileName: String
            contentResolver.query(uri!!, null, null, null, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    fileName = cursor.getString(nameIndex)
                } else {
                    throw IOException("Empty cursor returned for file.")
                }

                // Finding a Google Drive Sheet By Name (https://stackoverflow.com/a/43452641)
                val request: Drive.Files.List = drive!!.files().list()
                    .setPageSize(10) // Available Query parameters here:
                    //https://developers.google.com/drive/v3/web/search-parameters
                    .setQ("mimeType = 'application/vnd.google-apps.spreadsheet' and name contains '$fileName' and trashed = false")
                    .setFields("nextPageToken, files(id, name)")

                val result = request.execute()

                val files = result.files
                var spreadsheetId: String? = null
                if (files != null) {
                    for (file in files) {
                        spreadsheetId = file.id
                        Timber.e("spreadsheetId: %s", spreadsheetId)
                    }
                }
                callback(spreadsheetId)
            }
        }
    }

    fun getSheetData(sheetId: String, callback: ()->Unit) {
        if (!isInitialized()) {
            throw IOException("SheetHelper has not been initialized yet!!")
        }

        CoroutineScope(Dispatchers.IO).launch {
            val spreadsheet: Spreadsheet = sheets!!.spreadsheets()
                .get(sheetId)  // https://developers.google.com/sheets/api/reference/rest/v4/spreadsheets/get
                .setFields("sheets.properties,sheets.data.rowData.values.formattedValue,sheets.data.rowData.values.textFormatRuns")
                .execute()
            Timber.e("spreadsheet: $spreadsheet")
            val values = spreadsheet.values
            Timber.d("values(${values::class.simpleName}): $values")

            val data0 = spreadsheet.sheets[0].data[0]
            Timber.e("data[0](${data0::class.simpleName}): $data0")

            val properties = spreadsheet.sheets[0].properties
            Timber.e("properties(${properties::class.simpleName}): $properties")

            val rowData = data0.rowData
            for (row in rowData) {
                Timber.e("row(${row::class.simpleName}): $row")
            }
            callback()
        }
    }
}