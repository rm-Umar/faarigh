package com.faarigh.app.ui.screen.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faarigh.app.data.learn.LearnContentProvider
import com.faarigh.app.ui.component.RetroCard
import com.faarigh.app.ui.component.RetroHeading
import com.faarigh.app.ui.component.RetroOutlinedButton
import com.faarigh.app.ui.component.gridPaper
import com.faarigh.app.ui.theme.MonospaceFamily

@Composable
fun LearnArticleScreen(articleId: String, onBack: () -> Unit = {}) {
    val article = LearnContentProvider.articles.find { it.id == articleId }

    if (article == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .gridPaper(),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            Text(
                "Article not found",
                fontFamily = MonospaceFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val accentColor = Color(article.accentColor)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .gridPaper()
            .windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Back button
        item {
            RetroOutlinedButton(
                text = "\u2190  Back",
                onClick = onBack,
                modifier = Modifier,
            )
        }

        // Article header
        item {
            Column {
                Text(
                    "LEARN",
                    fontFamily = MonospaceFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 3.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    article.title,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    article.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = accentColor,
                )
            }
        }

        // Sections
        itemsIndexed(article.sections) { _, section ->
            Column {
                if (section.heading != null) {
                    RetroHeading(section.heading)
                    Spacer(Modifier.height(8.dp))
                }

                RetroCard {
                    Column {
                        Text(
                            text = section.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp,
                        )

                        if (section.citation != null) {
                            Spacer(Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(4.dp),
                                    )
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                    .padding(10.dp),
                            ) {
                                Text(
                                    text = section.citation,
                                    fontFamily = MonospaceFamily,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
