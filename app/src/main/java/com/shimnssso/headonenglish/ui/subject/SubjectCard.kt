package com.shimnssso.headonenglish.ui.subject

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.shimnssso.headonenglish.R
import com.shimnssso.headonenglish.room.DatabaseSubject

@Composable
fun SubjectCard(
    subject: DatabaseSubject,
    selected: Boolean,
    index: Int,
    onClick: (Int) -> Unit,
    onLongClick: (DatabaseSubject) -> Unit,
    onLinkClick: (String) -> Unit,
) {
    val border = if (selected) BorderStroke(3.dp, MaterialTheme.colors.primary) else null

    Card(
        border = border,
        modifier = Modifier
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onClick(index)
                    },
                    onLongPress = {
                        onLongClick(subject)
                    },
                )
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            SubjectImage(subject, onLinkClick)
            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = subject.title,
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (!subject.description.isNullOrEmpty()) {
                    Text(text = subject.description, style = MaterialTheme.typography.body2)
                }
            }
        }
    }
}

@Composable
private fun SubjectImage(
    subject: DatabaseSubject,
    onLinkClick: (String) -> Unit
) {
    if (subject.image != null) {
        Box {
            Image(
                painter = rememberImagePainter(
                    data = subject.image,
                    builder = {
                        crossfade(true)
                    }
                ),
                contentDescription = "image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(3.dp, MaterialTheme.colors.primary, CircleShape)
                    .clickable {
                        if (subject.link != null) {
                            onLinkClick(subject.link)
                        }
                    }
            )
            if (subject.link != null) {
                LinkMark(subject, onLinkClick)
            }
        }
    } else {
        Box {
            Image(
                painter = painterResource(R.drawable.ic_picture),
                contentDescription = "picture icon",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(3.dp, MaterialTheme.colors.primary, CircleShape)
                    .clickable(
                        onClick = {
                            if (subject.link != null) {
                                onLinkClick(subject.link)
                            }
                        })
                    .padding(32.dp)
            )
            if (subject.link != null) {
                LinkMark(subject, onLinkClick)
            }
        }
    }
}

@Composable
private fun LinkMark(
    subject: DatabaseSubject,
    onLinkClick: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .border(3.dp, MaterialTheme.colors.primary, CircleShape)
            .clickable {
                if (subject.link != null) {
                    onLinkClick(subject.link)
                }
            }
    ) {
        Text(
            "link",
            style = MaterialTheme.typography.caption,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.secondary,
            modifier = Modifier
                .background(Color.DarkGray.copy(alpha = 0.5f))
                .fillMaxWidth()
                .padding(4.dp)
        )
    }
}