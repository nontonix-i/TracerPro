package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AiRobotAvatar
import com.example.ui.components.DashboardSummaryChart
import com.example.ui.components.DrawerInventorySummary
import com.example.ui.components.MinimalStatCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppNavScreen
import com.example.ui.viewmodel.SfaViewModel
import com.example.ui.viewmodel.TransactionDialogState
import com.example.util.LocationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: SfaViewModel,
    modifier: Modifier = Modifier,
    onOpenDrawer: () -> Unit = {}
) {
    val context = LocalContext.current
    val warungs by viewModel.warungs.collectAsState()
    val rutes by viewModel.rutes.collectAsState()
    val drawers by viewModel.drawers.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val dailyLoadings by viewModel.dailyLoadings.collectAsState()
    val products by viewModel.products.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val personalAccounts by viewModel.personalAccounts.collectAsState()
    val personalDebts by viewModel.personalDebts.collectAsState()
    val lang by viewModel.appLanguage.collectAsState()

    val todayDateStr = remember(lang) {
        val locale = if (lang.equals("EN", ignoreCase = true)) Locale.US else Locale("id", "ID")
        val pattern = if (lang.equals("EN", ignoreCase = true)) "EEEE, dd MMM yyyy" else "EEEE, dd MMMM yyyy"
        SimpleDateFormat(pattern, locale).format(Date())
    }
    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    // Optimized Calculations
    val totalPoolGudang = remember(drawers) { drawers.sumOf { it.stokPoolGudangPcs } }
    val totalFresh = remember(drawers) { drawers.sumOf { it.stokFreshPabrikPcs } }
    val totalBsBelumSortir = remember(drawers) { drawers.sumOf { it.stokBsBelumSortirPcs } }
    val totalPribadiLayak = remember(drawers) { drawers.sumOf { it.stokPribadiLayakJualPcs } }
    val totalPribadiRusak = remember(drawers) { drawers.sumOf { it.stokPribadiRusakPcs } }

    val todayTx = remember(transactions, todayKey) {
        transactions.filter { it.tanggal == todayKey && it.warungId != "CLOSING_SALES" && it.jenis != "CLOSING_HARIAN" }
    }
    val totalKasHariIni = remember(todayTx) { todayTx.sumOf { it.uangDiterima } }
    val totalPiutangWarung = remember(warungs) { warungs.filter { it.status != "Blacklist" }.sumOf { it.saldoPiutang } }

    val todayLoadings = remember(dailyLoadings, todayKey) { dailyLoadings.filter { it.tanggal == todayKey } }
    val isClosingDone = remember(todayLoadings) { todayLoadings.isNotEmpty() && todayLoadings.all { it.statusClosing } }
    val tagihanPabrik = remember(isClosingDone, todayLoadings) {
        if (isClosingDone) {
            todayLoadings.sumOf { it.tagihanPabrikClosing }
        } else {
            todayLoadings.sumOf { it.potensiHutangPabrik }
        }
    }

    val penghasilanBersihSales = remember(todayTx, products) {
        todayTx.sumOf { tx ->
            val prod = products.find { it.id == tx.productId }
            val hBeliPerPcs = if (prod != null && prod.rasioKonversi > 0) prod.hargaBeliPabrik / prod.rasioKonversi else 0.0
            val marginPerPcs = (tx.hargaSatuan - hBeliPerPcs).coerceAtLeast(0.0)
            marginPerPcs * tx.pcsLaku
        }
    }

    val totalTitipBaruPcs = remember(todayTx) { todayTx.filter { it.jenis == "TITIP_BARU" }.sumOf { it.restockBaruPcs } }
    val overdueCount = remember(warungs) { warungs.count { it.saldoPiutang > 0 } }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Clean, Modern Screen Header
        Surface(
            color = Color.White,
            border = BorderStroke(1.dp, Slate200),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Slate100)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu Navigasi",
                            tint = Slate800,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = com.example.util.AppStrings.tr("Dashboard Operasional", "Operational Dashboard", lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = todayDateStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500,
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Slate100,
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.testTag("driver_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(EmeraldSuccess)
                        )
                        Text(
                            text = userProfile?.namaSalesman?.ifBlank { "Salesman" } ?: "Salesman",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Slate800,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 80.dp)
        ) {
            // 1. AI Copilot Card (Simple & Modern)
            item(key = "ai_copilot", contentType = "ai_card") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.openTransactionDialog(TransactionDialogState.AiCopilot) }
                        .testTag("ai_copilot_dashboard_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900, contentColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            AiRobotAvatar(
                                size = 40.dp,
                                containerBackground = Slate800,
                                accentColor = EmeraldPrimary
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "TracerPro AI Copilot",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Surface(
                                        color = EmeraldPrimary.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "OpenAI",
                                            color = EmeraldPrimary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = com.example.util.AppStrings.tr("Draf WA Bos, Analisis Piutang & Saran Restock", "WA Drafts, Debt Analysis & Restock Advice", lang),
                                    fontSize = 11.sp,
                                    color = Slate300,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = { viewModel.openTransactionDialog(TransactionDialogState.AiCopilot) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Slate800,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(com.example.util.AppStrings.tr("Tanya AI", "Ask AI", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Quick Actions (Aksi Cepat Lapangan)
            item(key = "quick_actions", contentType = "actions_row") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = com.example.util.AppStrings.tr("AKSI OPERASIONAL", "OPERATIONAL ACTIONS", lang),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionButton(
                            icon = Icons.Default.Inventory2,
                            title = com.example.util.AppStrings.tr("Terima Kiriman", "Receive Stock", lang),
                            subtitle = com.example.util.AppStrings.tr("Pool Rumah", "Weekly Pool", lang),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_terima_kiriman"),
                            onClick = {
                                viewModel.openTransactionDialog(TransactionDialogState.TerimaKirimanMingguan)
                            }
                        )

                        QuickActionButton(
                            icon = Icons.Default.Storefront,
                            title = com.example.util.AppStrings.tr("Kunjungan", "Visits", lang),
                            subtitle = com.example.util.AppStrings.tr("Titip & Bon", "Consign & Debt", lang),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_kunjungan_rute"),
                            onClick = {
                                viewModel.setScreen(AppNavScreen.TRANSAKSI)
                            }
                        )

                        QuickActionButton(
                            icon = Icons.Default.AccountBalanceWallet,
                            title = com.example.util.AppStrings.tr("Closing", "Closing", lang),
                            subtitle = com.example.util.AppStrings.tr("Setor Kas", "Settle Cash", lang),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_closing_sore"),
                            onClick = {
                                viewModel.openTransactionDialog(TransactionDialogState.ClosingSore)
                            }
                        )
                    }
                }
            }

            // 3. Performance Summary Chart
            item(key = "summary_chart", contentType = "chart") {
                DashboardSummaryChart(
                    todayTransactions = todayTx,
                    totalWarungsInRoute = warungs.size,
                    modifier = Modifier.testTag("dashboard_summary_chart"),
                    lang = lang
                )
            }

            // 4. Inventory Drawers
            item(key = "inventory_drawers", contentType = "drawers") {
                DrawerInventorySummary(
                    stokPoolGudang = totalPoolGudang,
                    stokFresh = totalFresh,
                    stokBsBelumSortir = totalBsBelumSortir,
                    stokPribadiLayak = totalPribadiLayak,
                    stokPribadiRusak = totalPribadiRusak,
                    onTerimaKirimanClick = {
                        viewModel.openTransactionDialog(TransactionDialogState.TerimaKirimanMingguan)
                    },
                    modifier = Modifier.testTag("inventory_drawer_card"),
                    lang = lang
                )
            }

            // 5. Key Financial Metrics
            item(key = "financial_metrics", contentType = "metrics") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = com.example.util.AppStrings.tr("RINGKASAN FINANSIAL", "FINANCIAL SUMMARY", lang),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MinimalStatCard(
                            title = com.example.util.AppStrings.tr("Kas Hari Ini", "Cash Collected", lang),
                            value = SfaViewModel.formatRupiah(totalKasHariIni),
                            subtitle = com.example.util.AppStrings.tr("${todayTx.size} transaksi outlet", "${todayTx.size} visits", lang),
                            icon = Icons.Default.Payments,
                            accentColor = EmeraldSuccess,
                            modifier = Modifier.weight(1f)
                        )

                        MinimalStatCard(
                            title = com.example.util.AppStrings.tr("Setoran Supplier", "Supplier Bill", lang),
                            value = SfaViewModel.formatRupiah(tagihanPabrik),
                            subtitle = if (isClosingDone) com.example.util.AppStrings.tr("Lunas Closing", "Reconciled", lang) else com.example.util.AppStrings.tr("Target Estimasi", "Target Est.", lang),
                            icon = Icons.Default.Factory,
                            accentColor = Slate800,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MinimalStatCard(
                            title = com.example.util.AppStrings.tr("Total Bon Outlet", "Total Outlet Debt", lang),
                            value = SfaViewModel.formatRupiah(totalPiutangWarung),
                            subtitle = com.example.util.AppStrings.tr("$overdueCount outlet piutang", "$overdueCount in debt", lang),
                            icon = Icons.Default.ReceiptLong,
                            accentColor = AmberWarning,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.setScreen(AppNavScreen.LAPORAN)
                            }
                        )

                        MinimalStatCard(
                            title = com.example.util.AppStrings.tr("Penghasilan Sales", "Sales Earnings", lang),
                            value = SfaViewModel.formatRupiah(penghasilanBersihSales),
                            subtitle = com.example.util.AppStrings.tr("Margin Hak Bersih", "Net Profit", lang),
                            icon = Icons.Default.TrendingUp,
                            accentColor = Color(0xFF2563EB),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 6. Personal Finance Card
            item(key = "personal_finance", contentType = "personal_finance") {
                val totalKasPribadi = remember(personalAccounts) {
                    personalAccounts.filter { !it.isPaylater }.sumOf { it.saldo }
                }
                val totalTagihanPaylater = remember(personalAccounts) {
                    personalAccounts.filter { it.isPaylater }.sumOf { it.saldo }
                }
                val totalPiutangTeman = remember(personalDebts) {
                    personalDebts.filter { it.jenis == "PIUTANG_SAYA" && it.status != "LUNAS" }.sumOf { it.sisaNominal }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setScreen(AppNavScreen.KEUANGAN_PRIBADI) }
                        .testTag("dashboard_personal_finance_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = SlateBorderStroke
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFEFF6FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = com.example.util.AppStrings.tr("DOMPET & KEUANGAN PRIBADI", "PERSONAL FINANCE & WALLET", lang),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900
                                    )
                                    Text(
                                        text = com.example.util.AppStrings.tr("Saldo kas, hutang piutang & paylater", "Cash, debts & paylater", lang),
                                        fontSize = 11.sp,
                                        color = Slate500
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        HorizontalDivider(color = Slate100, thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(com.example.util.AppStrings.tr("Kas & Bank", "Cash & Bank", lang), fontSize = 10.sp, color = Slate500)
                                Text(
                                    text = SfaViewModel.formatRupiah(totalKasPribadi),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSuccess
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(com.example.util.AppStrings.tr("Paylater", "Paylater", lang), fontSize = 10.sp, color = Slate500)
                                Text(
                                    text = SfaViewModel.formatRupiah(totalTagihanPaylater),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalTagihanPaylater > 0) AmberWarning else Slate700
                                )
                            }

                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text(com.example.util.AppStrings.tr("Piutang Teman", "Loans Given", lang), fontSize = 10.sp, color = Slate500)
                                Text(
                                    text = SfaViewModel.formatRupiah(totalPiutangTeman),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2563EB)
                                )
                            }
                        }
                    }
                }
            }

            // 7. Active Route Progress
            item(key = "active_route", contentType = "route_card") {
                val todayRute = remember(rutes) { SfaViewModel.findRuteForToday(rutes) }
                val activeRute = todayRute ?: rutes.firstOrNull()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (todayRute != null) {
                                viewModel.setSelectedRute(todayRute.id, fromUser = true)
                            }
                            viewModel.setScreen(AppNavScreen.TRANSAKSI)
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = SlateBorderStroke
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (todayRute != null && activeRute?.id == todayRute.id) EmeraldSurface else Slate100),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AltRoute,
                                        contentDescription = null,
                                        tint = if (todayRute != null && activeRute?.id == todayRute.id) EmeraldSuccess else Slate800,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = activeRute?.namaRute ?: com.example.util.AppStrings.tr("Jalur 1 - Pasar Rebo & Cibubur", "Route 1", lang),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate900
                                        )
                                        if (todayRute != null && activeRute?.id == todayRute.id) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = EmeraldSuccess
                                            ) {
                                                Text(
                                                    text = com.example.util.AppStrings.tr("Hari Ini", "Today", lang),
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${activeRute?.hariKunjungan ?: "Senin"} • ${activeRute?.jarakTotalKm ?: 18.5} km",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate500,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Slate100,
                                border = BorderStroke(1.dp, Slate200)
                            ) {
                                Text(
                                    text = "${todayTx.size}/50 ${com.example.util.AppStrings.tr("Toko", "Stores", lang)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate800,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { (todayTx.size / 50f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = Slate900,
                            trackColor = Slate100
                        )

                        val routeWarungs = remember(warungs, activeRute) {
                            if (activeRute != null) {
                                warungs.filter { it.ruteId == activeRute.id && it.status == "Aktif" }
                            } else {
                                warungs.filter { it.status == "Aktif" }
                            }
                        }
                        val mappedCount = routeWarungs.count { it.latitude != 0.0 || it.longitude != 0.0 }

                        Button(
                            onClick = {
                                LocationHelper.openMultiStopGoogleMapsRoute(
                                    context = context,
                                    warungs = routeWarungs,
                                    routeTitle = activeRute?.namaRute ?: com.example.util.AppStrings.tr("Rute Hari Ini", "Today's Route", lang)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate900),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("btn_start_route_navigation"),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Directions,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${com.example.util.AppStrings.tr("Mulai Navigasi Rute di Google Maps", "Start Route Navigation in Google Maps", lang)} ($mappedCount)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // 8. Modal Tertanam Info
            if (tagihanPabrik > totalKasHariIni && totalKasHariIni > 0) {
                item {
                    val kurangSetor = tagihanPabrik - totalKasHariIni
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AmberSurface),
                        border = BorderStroke(1.dp, AmberBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "${com.example.util.AppStrings.tr("Info Alokasi Kas & Modal Tertanam", "Cash Allocation Info", lang)}: ${SfaViewModel.formatRupiah(kurangSetor)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberText
                                )
                                Text(
                                    text = if (totalTitipBaruPcs > 0) {
                                        com.example.util.AppStrings.tr(
                                            "Terdapat $totalTitipBaruPcs Pcs Titip Baru di warung hari ini. Selisih kas ini adalah modal tertanam Anda.",
                                            "There are $totalTitipBaruPcs new drop units placed today.",
                                            lang
                                        )
                                    } else {
                                        com.example.util.AppStrings.tr(
                                            "Kas warung terkumpul hari ini belum menutup setoran produk supplier. Pastikan closing sore sesuai fisik mobil.",
                                            "Collected outlet cash has not fully covered supplier bill yet.",
                                            lang
                                        )
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AmberText.copy(alpha = 0.9f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .heightIn(min = 90.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
            contentColor = Slate900
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Slate200)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate100),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Slate800,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Slate500,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
