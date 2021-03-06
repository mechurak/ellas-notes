package com.shimnssso.headonenglish.googlesheet

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.CellData
import com.google.api.services.sheets.v4.model.GridData
import com.google.api.services.sheets.v4.model.RowData
import com.google.api.services.sheets.v4.model.Sheet
import com.google.api.services.sheets.v4.model.SheetProperties
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.gson.GsonBuilder
import com.shimnssso.headonenglish.room.DatabaseCard
import com.shimnssso.headonenglish.room.DatabaseLecture
import com.shimnssso.headonenglish.room.DatabaseSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException

class SheetException(message: String) : Exception(message)

object SheetHelper {
    private var drive: Drive? = null
    private var sheets: Sheets? = null

    private val gson = GsonBuilder().create()

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

    suspend fun getFilesFromUri(contentResolver: ContentResolver, uri: Uri): List<File> {
        if (!isInitialized()) {
            throw IOException("SheetHelper has not been initialized yet!!")
        }
        var retFiles: List<File> = listOf()
        withContext(Dispatchers.IO) {
            var fileName: String
            contentResolver.query(uri, null, null, null, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    fileName = cursor.getString(nameIndex)
                } else {
                    throw IOException("Empty cursor returned for file.")
                }
            }

            // Finding a Google Drive Sheet By Name (https://stackoverflow.com/a/43452641)
            val request: Drive.Files.List = drive!!.files().list()
                .setPageSize(10) // Available Query parameters here:
                //https://developers.google.com/drive/v3/web/search-parameters
                .setQ("mimeType = 'application/vnd.google-apps.spreadsheet' and name contains '$fileName' and trashed = false")
                .setFields("nextPageToken, files(id, name, owners, modifiedTime)")

            val result = request.execute()

            val files = result.files
            if (files != null) {
                retFiles = files
                for (file in files) {
                    Timber.d("file: $file")
                    Timber.d("file.modifiedTime: ${file.modifiedTime}")
                    Timber.d("file.owners ${file.owners}")
                    Timber.d("spreadsheetId: %s", file.id)
                }
            }
        }
        return retFiles
    }

    suspend fun fetchSpreadsheet(spreadsheetId: String): Spreadsheet {
        if (!isInitialized()) {
            throw IOException("SheetHelper has not been initialized yet!!")
        }

        var spreadsheet: Spreadsheet
        withContext(Dispatchers.IO) {
            try {
                spreadsheet = sheets!!.spreadsheets()
                    .get(spreadsheetId)  // https://developers.google.com/sheets/api/reference/rest/v4/spreadsheets/get
                    .setFields("sheets.properties,sheets.data.rowData.values.formattedValue,sheets.data.rowData.values.textFormatRuns,sheets.data.rowData.values.effectiveFormat.textFormat.bold")
                    .execute()

                for ((index, sheet: Sheet) in spreadsheet.sheets.withIndex()) {
                    val properties: SheetProperties = sheet.properties
                    Timber.d("sheet[$index]: $properties")
                }
            } catch (e: Exception) {
                Timber.e(e)
                throw SheetException("Failed to fetch the doc. Please check your doc and guide page.")
            }
        }
        return spreadsheet
    }

    fun getLectureCardListPair(
        spreadsheet: Spreadsheet,
        subject: DatabaseSubject,
        remainedLectureMap: MutableMap<String, DatabaseLecture>,
        newLectures: MutableList<DatabaseLecture>,
        updateLectures: MutableList<DatabaseLecture>,
        newCards: MutableList<DatabaseCard>
    ): DatabaseSubject? {
        var retSubject: DatabaseSubject? = null
        for (sheet: Sheet in spreadsheet.sheets) {
            val sheetProperties: SheetProperties = sheet.properties
            Timber.i("sheetProperties: $sheetProperties")
            val sheetTitle = sheetProperties.title
            if (sheetTitle.startsWith("doc_info")) {
                val data: GridData = sheet.data[0]  // We didn't query for multi section.
                retSubject = getSubjectInfo(data, subject)
                continue
            } else if (sheetTitle.endsWith("_temp")) {
                Timber.d("skip $sheetTitle sheet")
                continue
            }

            val frozenRowCount = sheetProperties.gridProperties.frozenRowCount
            if (frozenRowCount != 2) {
                Timber.e("unexpected frozenRowCount: $frozenRowCount")
                throw SheetException("Unexpected frozen row count. Please check your doc and guide page.")
            }

            val data: GridData = sheet.data[0]  // We didn't query for multi section.
            val idxHolder = IndexHolder()

            val rowDataList: List<RowData> = data.rowData
            var prevLecture: DatabaseLecture? = null
            for ((i, rowData: RowData) in rowDataList.withIndex()) {
                val cells = rowData.getValues()
                if (cells == null || cells.size < idxHolder.order) {
                    Timber.w("wrong row[$i] cells: ${cells}")
                    continue
                }
                if (i < frozenRowCount) {
                    idxHolder.setColumnIndices(rowData)
                } else if (cells[idxHolder.order].formattedValue == null) {
                    continue
                } else if (cells[idxHolder.order].formattedValue == "0") {
                    if (prevLecture != null) {
                        val originLecture = remainedLectureMap[prevLecture.date]
                        if (originLecture == null) {
                            newLectures.add(prevLecture)
                        } else {
                            val updateLecture: DatabaseLecture = originLecture.copy(
                                title = prevLecture.title,
                                category = prevLecture.category,
                                remoteUrl = prevLecture.remoteUrl,
                                link1 = prevLecture.link1,
                                link2 = prevLecture.link2,
                                quizCount = prevLecture.quizCount
                            )
                            updateLectures.add(updateLecture)
                            remainedLectureMap.remove(prevLecture.date)
                        }
                    }
                    prevLecture = getLecture(idxHolder, cells, subject.subjectId)
                } else {
                    val card = getCard(idxHolder, cells, subject.subjectId)
                    if (card.isForQuiz > 0 && prevLecture != null && card.date == prevLecture.date) {
                        prevLecture.quizCount += 1
                    }
                    newCards.add(card)
                }
            }
            if (prevLecture != null) {
                val originLecture = remainedLectureMap[prevLecture.date]
                if (originLecture == null) {
                    newLectures.add(prevLecture)
                } else {
                    val updateLecture: DatabaseLecture = originLecture.copy(
                        title = prevLecture.title,
                        category = prevLecture.category,
                        remoteUrl = prevLecture.remoteUrl,
                        link1 = prevLecture.link1,
                        link2 = prevLecture.link2,
                        quizCount = prevLecture.quizCount
                    )
                    updateLectures.add(updateLecture)
                    remainedLectureMap.remove(prevLecture.date)
                }
            }
        }
        return retSubject
    }

    private fun getSubjectInfo(data: GridData, subject: DatabaseSubject): DatabaseSubject {
        val rowDataList: List<RowData> = data.rowData
        var description: String? = null
        var link: String? = null
        var subjectForUrl: String? = null
        var image: String? = null
        for ((i, rowData: RowData) in rowDataList.withIndex()) {
            Timber.d("[$i] $rowData")
            val cells = rowData.getValues() ?: break
            if (cells.size < 2) {
                Timber.e("unexpected cell.size: ${cells.size}. row $i")
                continue
            }
            when (cells[0].formattedValue) {
                "key" -> {
                }
                "description" -> description = cells[1].formattedValue
                "link" -> link = cells[1].formattedValue
                "subjectForUrl" -> {
                    subjectForUrl = cells[1].formattedValue
                    if (subjectForUrl == "N/A") subjectForUrl = null
                }
                "image" -> image = cells[1].formattedValue
                else -> {
                    Timber.w("unexpected key. ${cells[0].formattedValue}")
                }
            }
        }
        return subject.copy(
            description = description,
            link = link,
            subjectForUrl = subjectForUrl,
            image = image,
            lastUpdateTime = System.currentTimeMillis()
        )
    }

    private fun getLecture(
        idx: IndexHolder,
        cells: List<CellData>,
        subjectId: Int
    ): DatabaseLecture {
        val category =
            if (idx.metaCategory > 0 && cells.size > idx.metaCategory) cells[idx.metaCategory].formattedValue else null
        val remoteUrl =
            if (idx.metaRemoteUrl > 0 && cells.size > idx.metaRemoteUrl) cells[idx.metaRemoteUrl].formattedValue else null
        val link1 =
            if (idx.metaLink1 > 0 && cells.size > idx.metaLink1) cells[idx.metaLink1].formattedValue else null
        val link2 =
            if (idx.metaLink2 > 0 && cells.size > idx.metaLink2) cells[idx.metaLink2].formattedValue else null
        return DatabaseLecture(
            subjectId = subjectId,
            date = cells[idx.date].formattedValue!!,
            title = cells[idx.metaTitle].formattedValue!!,
            category = category,
            remoteUrl = remoteUrl,
            localUrl = null,
            link1 = link1,
            link2 = link2,
        )
    }

    private fun getCard(idx: IndexHolder, cells: List<CellData>, subjectId: Int): DatabaseCard {
        val hint =
            if (idx.hint > 0 && cells.size > idx.hint) cells[idx.hint].formattedValue else null
        val note =
            if (idx.note > 0 && cells.size > idx.note) cells[idx.note].formattedValue else null
        val memo =
            if (idx.memo > 0 && cells.size > idx.memo) cells[idx.memo].formattedValue else null
        val quizTemp =
            if (idx.quiz > 0 && cells.size > idx.quiz) cells[idx.quiz].formattedValue else null
        var quiz = 0
        if (quizTemp != null) {
            quiz = quizTemp.toInt()
        }
        // Timber.e(cells[idx.text].toPrettyString())
        return DatabaseCard(
            subjectId = subjectId,
            date = cells[idx.date].formattedValue!!,
            order = cells[idx.order].formattedValue!!.toInt(),
            text = gson.toJson(cells[idx.text]),
            hint = hint,
            note = note,
            memo = memo,
            isForQuiz = quiz
        )
    }

    suspend fun updateRemoteUrl(
        subjectForUrl: String,
        lectures: List<DatabaseLecture>
    ): List<DatabaseLecture> {
        if (!isInitialized()) {
            throw IOException("SheetHelper has not been initialized yet!!")
        }

        // https://docs.google.com/spreadsheets/d/1vcC8BtRiDmh1tv8IzoNonXvRrTXQHRJOJmD0O1EgnMk/edit?usp=sharing
        val spreadsheetId = "1vcC8BtRiDmh1tv8IzoNonXvRrTXQHRJOJmD0O1EgnMk"

        val retLectures = mutableListOf<DatabaseLecture>()

        val urlMap = mutableMapOf<String, String>()

        var spreadsheet: Spreadsheet
        withContext(Dispatchers.IO) {
            try {
                spreadsheet = sheets!!.spreadsheets()
                    .get(spreadsheetId)  // https://developers.google.com/sheets/api/reference/rest/v4/spreadsheets/get
                    .setRanges(listOf("$subjectForUrl!A2:B"))
                    .setFields("sheets.properties,sheets.data.rowData.values.formattedValue")
                    .execute()

                for ((index, sheet: Sheet) in spreadsheet.sheets.withIndex()) {
                    val properties: SheetProperties = sheet.properties
                    Timber.i("sheet[$index]: $properties")

                    val rowDataList = sheet.data[0].rowData
                    for ((i, rowData: RowData) in rowDataList.withIndex()) {
                        val cells = rowData.getValues()
                        // Timber.e("[$i] $cells")
                        val date = cells[0].formattedValue
                        val remoteUrl = cells[1].formattedValue
                        urlMap[date] = remoteUrl
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
                throw SheetException("Failed to get remoteUrls. Please check your 'subjectForUrl' field on 'doc_info' sheet")
            }
        }

        if (urlMap.isNotEmpty()) {
            for (lecture in lectures) {
                retLectures.add(lecture.copy(remoteUrl = urlMap[lecture.date]))
            }
        }

        return retLectures
    }
}