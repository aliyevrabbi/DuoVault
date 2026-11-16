package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Vault
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosGreen
import com.example.ui.theme.IosPillTrack
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MyCardBlueEnd
import com.example.ui.theme.MyCardBlueStart
import com.example.ui.theme.ObsidianLeatherInner
import com.example.ui.theme.ObsidianLeatherOuter
import com.example.ui.theme.ObsidianLeatherPocketBottom
import com.example.ui.theme.ObsidianLeatherPocketTop
import com.example.ui.theme.ObsidianStitch
import com.example.ui.theme.PartnerCardCrimsonEnd
import com.example.ui.theme.PartnerCardCrimsonStart
import java.util.Locale

private val WalletOuterShape = RoundedCornerShape(30.dp)
private val CardInnerShape = RoundedCornerShape(20.dp)

@Composable
fun Wallet3DCard(
    vault: Vault,
    isBalanceHidden: Boolean,
    selectedCardIndex: Int,
    depositAnimTrigger: Long,
    onToggleBalanceVisibility: () -> Unit,
    onSelectCard: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var targetTiltX by remember { mutableFloatStateOf(0f) }
    var targetTiltY by remember { mutableFloatStateOf(0f) }

    val animatedTiltX by animateFloatAsState(
        targetValue = targetTiltX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tiltX"
    )
    val animatedTiltY by animateFloatAsState(
        targetValue = targetTiltY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tiltY"
    )

    val coinDropAnim = remember { Animatable(0f) }
    LaunchedEffect(depositAnimTrigger) {
        if (depositAnimTrigger > 0L) {
            targetTiltX = -6f
            coinDropAnim.snapTo(1f)
            coinDropAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
            )
            targetTiltX = 0f
        }
    }

    val ownerSharePct = remember(vault.totalSaved, vault.ownerSaved) {
        if (vault.totalSaved > 0) ((vault.ownerSaved / vault.totalSaved) * 100).toInt() else 50
    }
    val partnerSharePct = 100 - ownerSharePct

    val outerBrush = remember {
        Brush.verticalGradient(listOf(ObsidianLeatherOuter, ObsidianLeatherInner))
    }
    val ownerCardBrush = remember {
        Brush.horizontalGradient(listOf(MyCardBlueStart, MyCardBlueEnd))
    }
    val partnerCardBrush = remember {
        Brush.horizontalGradient(listOf(PartnerCardCrimsonStart, PartnerCardCrimsonEnd))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(244.dp)
            .testTag("wallet_3d_container")
            .graphicsLayer {
                rotationX = animatedTiltX
                rotationY = animatedTiltY
                cameraDistance = 16f * density
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        targetTiltY = ((offset.x - centerX) / centerX) * 5.5f
                        targetTiltX = -((offset.y - centerY) / centerY) * 5.5f
                        tryAwaitRelease()
                        targetTiltX = 0f
                        targetTiltY = 0f
                    }
                )
            }
            .shadow(
                elevation = 16.dp,
                shape = WalletOuterShape,
                spotColor = Color.Black.copy(alpha = 0.38f)
            )
            .clip(WalletOuterShape)
            .background(outerBrush)
            .border(1.dp, Color.White.copy(alpha = 0.08f), WalletOuterShape)
            .drawWithCache {
                val dashStroke = Stroke(
                    width = 1.3.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                )
                val pad = 14.dp.toPx()
                val cr = 22.dp.toPx()
                onDrawBehind {
                    drawRoundRect(
                        color = ObsidianStitch.copy(alpha = 0.5f),
                        topLeft = Offset(pad, pad),
                        size = Size(size.width - pad * 2, size.height - pad * 2),
                        cornerRadius = CornerRadius(cr, cr),
                        style = dashStroke
                    )
                }
            }
            .padding(12.dp)
    ) {
        // Peeking Card 2 (Back): Partner's Card
        val partnerCardOffset by animateDpAsState(
            targetValue = if (selectedCardIndex == 1) 8.dp else 20.dp,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "partnerCardOffset"
        )
        val partnerDisplay = vault.partnerName.ifBlank { "Dostum" }
        PeekingSavingsCard(
            title = partnerDisplay,
            subtitle = "Payı: $partnerSharePct%",
            rightText = formatMoney(vault.partnerSaved, vault.currency, isBalanceHidden),
            brush = partnerCardBrush,
            isSelected = selectedCardIndex == 1,
            onClick = { onSelectCard(if (selectedCardIndex == 1) 0 else 1) },
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(92.dp)
                .align(Alignment.TopCenter)
                .offset(y = partnerCardOffset)
                .testTag("wallet_card_partner")
        )

        // Peeking Card 1 (Front): My Card
        val ownerCardOffset by animateDpAsState(
            targetValue = if (selectedCardIndex == 0) 26.dp else 44.dp,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "ownerCardOffset"
        )
        PeekingSavingsCard(
            title = vault.ownerName.ifBlank { "Mən" },
            subtitle = "Payı: $ownerSharePct% • ${vault.inviteCode}",
            rightText = formatMoney(vault.ownerSaved, vault.currency, isBalanceHidden),
            brush = ownerCardBrush,
            isSelected = selectedCardIndex == 0,
            onClick = { onSelectCard(0) },
            showMastercardCircles = true,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .height(98.dp)
                .align(Alignment.TopCenter)
                .offset(y = ownerCardOffset)
                .testTag("wallet_card_owner")
        )

        // Animated Deposit Badge dropping into the 3D Wallet Pocket
        if (coinDropAnim.value > 0.01f) {
            val dropY = ((1f - coinDropAnim.value) * 82f).dp
            Surface(
                shape = CircleShape,
                color = IosGreen,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = dropY)
                    .graphicsLayer {
                        alpha = coinDropAnim.value
                        val scale = 0.85f + 0.2f * coinDropAnim.value
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                Text(
                    text = "+ ƏMANƏT ƏLAVƏ OLUNDU",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        // Front Stitched Leather Pocket with Cached Geometry (Zero Per-Frame Allocation)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.BottomCenter)
                .drawWithCache {
                    val w = size.width
                    val h = size.height
                    val corner = 22.dp.toPx()
                    val notchWidth = w * 0.34f
                    val notchDepth = 22.dp.toPx()
                    val notchStart = (w - notchWidth) / 2f
                    val notchEnd = notchStart + notchWidth

                    val pocketPath = Path().apply {
                        moveTo(corner, 0f)
                        lineTo(notchStart, 0f)
                        cubicTo(
                            notchStart + notchWidth * 0.22f, 0f,
                            notchStart + notchWidth * 0.25f, notchDepth,
                            w / 2f, notchDepth
                        )
                        cubicTo(
                            notchEnd - notchWidth * 0.25f, notchDepth,
                            notchEnd - notchWidth * 0.22f, 0f,
                            notchEnd, 0f
                        )
                        lineTo(w - corner, 0f)
                        quadraticTo(w, 0f, w, corner)
                        lineTo(w, h - corner)
                        quadraticTo(w, h, w - corner, h)
                        lineTo(corner, h)
                        quadraticTo(0f, h, 0f, h - corner)
                        lineTo(0f, corner)
                        quadraticTo(0f, 0f, corner, 0f)
                        close()
                    }

                    val inset = 9.dp.toPx()
                    val stitchPath = Path().apply {
                        moveTo(corner, inset)
                        lineTo(notchStart - 4.dp.toPx(), inset)
                        cubicTo(
                            notchStart + notchWidth * 0.20f, inset,
                            notchStart + notchWidth * 0.25f, notchDepth + inset,
                            w / 2f, notchDepth + inset
                        )
                        cubicTo(
                            notchEnd - notchWidth * 0.25f, notchDepth + inset,
                            notchEnd - notchWidth * 0.20f, inset,
                            notchEnd + 4.dp.toPx(), inset
                        )
                        lineTo(w - corner, inset)
                        quadraticTo(w - inset, inset, w - inset, corner)
                        lineTo(w - inset, h - corner)
                        quadraticTo(w - inset, h - inset, w - corner, h - inset)
                        lineTo(corner, h - inset)
                        quadraticTo(inset, h - inset, inset, h - corner)
                        lineTo(inset, corner)
                        quadraticTo(inset, inset, corner, inset)
                        close()
                    }

                    val pocketBrush = Brush.verticalGradient(
                        colors = listOf(ObsidianLeatherPocketTop, ObsidianLeatherPocketBottom)
                    )
                    val lipStroke = Stroke(width = 1.5.dp.toPx())
                    val stitchStroke = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(13f, 9f), 0f)
                    )
                    val rivetRadius = 6.5.dp.toPx()
                    val rivetStroke = Stroke(width = 1.dp.toPx())
                    val leftRivet = Offset(26.dp.toPx(), 24.dp.toPx())
                    val rightRivet = Offset(w - 26.dp.toPx(), 24.dp.toPx())

                    onDrawBehind {
                        drawPath(path = pocketPath, brush = pocketBrush)
                        drawPath(
                            path = pocketPath,
                            color = Color.White.copy(alpha = 0.08f),
                            style = lipStroke
                        )
                        clipPath(pocketPath) {
                            drawPath(
                                path = stitchPath,
                                color = ObsidianStitch.copy(alpha = 0.75f),
                                style = stitchStroke
                            )
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.35f),
                                radius = rivetRadius,
                                center = leftRivet
                            )
                            drawCircle(
                                color = ObsidianStitch.copy(alpha = 0.5f),
                                radius = rivetRadius,
                                center = leftRivet,
                                style = rivetStroke
                            )
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.35f),
                                radius = rivetRadius,
                                center = rightRivet
                            )
                            drawCircle(
                                color = ObsidianStitch.copy(alpha = 0.5f),
                                radius = rivetRadius,
                                center = rightRivet,
                                style = rivetStroke
                            )
                        }
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = vault.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.68f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatMoney(vault.totalSaved, vault.currency, isBalanceHidden),
                        style = MaterialTheme.typography.displayMedium,
                        fontSize = 30.sp,
                        color = Color.White,
                        modifier = Modifier.testTag("wallet_total_balance_text")
                    )
                }

                IconButton(
                    onClick = onToggleBalanceVisibility,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                        .testTag("toggle_balance_visibility_button")
                ) {
                    Icon(
                        imageVector = if (isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Balansı gizlət və ya göstər",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PeekingSavingsCard(
    title: String,
    subtitle: String,
    rightText: String,
    brush: Brush,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showMastercardCircles: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(CardInnerShape)
            .background(brush)
            .border(
                width = if (isSelected) 1.4.dp else 0.8.dp,
                color = Color.White.copy(alpha = if (isSelected) 0.5f else 0.2f),
                shape = CardInnerShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showMastercardCircles) {
                    Box(modifier = Modifier.width(26.dp).height(16.dp)) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEB001B))
                        )
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .offset(x = 10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF79E1B).copy(alpha = 0.9f))
                        )
                    }
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.82f)
                    )
                }
            }

            Text(
                text = rightText,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun IosHatchedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    activeColor: Color = IosBlue,
    trackColor: Color = IosPillTrack
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "hatchedProgress"
    )

    val activeBrush = remember(activeColor) {
        Brush.horizontalGradient(listOf(activeColor.copy(alpha = 0.65f), activeColor))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(CircleShape)
            .background(trackColor)
            .padding(4.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        ) {
            val step = 9.dp.toPx()
            val strokeWidth = 2.dp.toPx()
            var x = -size.height
            while (x < size.width + size.height) {
                drawLine(
                    color = activeColor.copy(alpha = 0.25f),
                    start = Offset(x, size.height),
                    end = Offset(x + size.height * 0.7f, 0f),
                    strokeWidth = strokeWidth
                )
                x += step
            }
        }

        val fillFraction = animatedProgress.coerceAtLeast(0.06f)
        Box(
            modifier = Modifier
                .fillMaxWidth(fillFraction)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(activeBrush)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .align(Alignment.CenterEnd)
                    .padding(end = 1.dp)
                    .clip(CircleShape)
                    .background(activeColor)
                    .border(2.5.dp, Color.White, CircleShape)
            )
        }
    }
}

fun formatMoney(amount: Double, currency: String, isHidden: Boolean = false): String {
    if (isHidden) return "$currency ••••••"
    return "$currency${String.format(Locale.US, "%,.2f", amount)}"
}
