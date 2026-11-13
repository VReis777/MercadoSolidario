import { useEffect, useState } from 'react'
import {
  Alert, Box, Card, CardContent, CircularProgress, Divider, Grid, Paper, Stack,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography,
} from '@mui/material'
import PageShell from '../components/PageShell'
import campaignService, { CampaignOverview } from '../services/campaignService'
import { dashboardService, DashboardSummary } from '../services/dashboardService'
import { familyStatusLabels } from '../components/audit/RegistrationsTab'
import { getApiErrorMessage } from '../utils/apiError'
import siteStatsService, { SiteStatsSummary } from '../services/siteStatsService'

const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

function ReportsPage() {
  const [overview, setOverview] = useState<CampaignOverview | null>(null)
  const [summary, setSummary] = useState<DashboardSummary | null>(null)
  const [siteStats, setSiteStats] = useState<SiteStatsSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    const year = new Date().getFullYear()
    const from = `${year}-01-01`
    const to = `${year}-12-31`
    Promise.all([
      campaignService.overview(),
      dashboardService.summary(from, to),
      siteStatsService.summary().catch(() => null),
    ])
      .then(([o, s, st]) => { setOverview(o); setSummary(s); setSiteStats(st) })
      .catch((e) => setError(getApiErrorMessage(e, 'Não foi possível carregar os relatórios.')))
      .finally(() => setLoading(false))
  }, [])

  const historyYears = Object.keys(overview?.historicalByYear ?? {}).map(Number).sort()

  return (
    <PageShell title="Relatórios">
      {loading ? (
        <Box sx={{ textAlign: 'center', py: 6 }}><CircularProgress /></Box>
      ) : error ? (
        <Alert severity="error">{error}</Alert>
      ) : overview && summary ? (
        <Stack spacing={3}>
          <Grid container spacing={2}>
            <Grid item xs={12} md={4}>
              <Card><CardContent>
                <Typography variant="h6" gutterBottom>Famílias</Typography>
                <Typography variant="h4" color="primary.main" fontWeight={700}>{summary.families.total}</Typography>
                <Typography variant="body2" color="text.secondary">
                  cadastradas no total · {summary.families.createdInPeriod} em {overview.campaignYear}
                </Typography>
                <Divider sx={{ my: 1.5 }} />
                {Object.entries(summary.families.byStatus).map(([status, count]) => (
                  <Stack key={status} direction="row" justifyContent="space-between">
                    <Typography variant="body2">{familyStatusLabels[status] ?? status}</Typography>
                    <Typography variant="body2" fontWeight={600}>{count}</Typography>
                  </Stack>
                ))}
              </CardContent></Card>
            </Grid>
            <Grid item xs={12} md={4}>
              <Card><CardContent>
                <Typography variant="h6" gutterBottom>Estoque</Typography>
                <Typography variant="h4" color="primary.main" fontWeight={700}>{summary.stock.activeProducts}</Typography>
                <Typography variant="body2" color="text.secondary">
                  itens ativos · {summary.stock.lowStockProducts} abaixo do mínimo
                </Typography>
                <Divider sx={{ my: 1.5 }} />
                <Typography variant="body2" color="text.secondary">
                  Sem estoque nesta campanha: {overview.productsWithoutStock}
                </Typography>
              </CardContent></Card>
            </Grid>
            <Grid item xs={12} md={4}>
              <Card><CardContent>
                <Typography variant="h6" gutterBottom>Doações em {overview.campaignYear}</Typography>
                {summary.donations ? (
                  <>
                    <Typography variant="h4" color="primary.main" fontWeight={700}>
                      {currency.format(summary.donations.confirmedFinancialAmount)}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      {summary.donations.created} doações criadas · {summary.donations.awaitingPayment} aguardando
                    </Typography>
                    {summary.cash && (
                      <>
                        <Divider sx={{ my: 1 }} />
                        <Stack spacing={0.5}>
                          <Stack direction="row" justifyContent="space-between">
                            <Typography variant="body2">Entradas no caixa</Typography>
                            <Typography variant="body2" fontWeight={600} color="success.main">
                              {currency.format(summary.cash.entries)}
                            </Typography>
                          </Stack>
                          <Stack direction="row" justifyContent="space-between">
                            <Typography variant="body2">Saídas do caixa</Typography>
                            <Typography variant="body2" fontWeight={600} color="error.main">
                              {currency.format(summary.cash.exits)}
                            </Typography>
                          </Stack>
                          <Typography variant="caption" color="text.secondary">
                            {summary.cash.movements} movimentação(ões) manual(is) — inclui doações registradas direto no caixa
                          </Typography>
                        </Stack>
                      </>
                    )}
                  </>
                ) : (
                  <Typography variant="body2" color="text.secondary">Módulo de doações sem dados no período.</Typography>
                )}
              </CardContent></Card>
            </Grid>
            <Grid item xs={12} md={4}>
              <Card><CardContent>
                <Typography variant="h6" gutterBottom>Acessos ao site</Typography>
                {siteStats ? (
                  <>
                    <Typography variant="h4" color="primary.main" fontWeight={700}>
                      {siteStats.total.toLocaleString('pt-BR')}
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                      visitas registradas (1 por página por sessão)
                    </Typography>
                    {siteStats.pages.slice(0, 4).map((page) => (
                      <Stack key={page.path} direction="row" justifyContent="space-between">
                        <Typography variant="body2">{page.path === '/' ? 'Início' : page.path}</Typography>
                        <Typography variant="body2" fontWeight={600}>{page.views.toLocaleString('pt-BR')}</Typography>
                      </Stack>
                    ))}
                  </>
                ) : (
                  <Typography variant="body2" color="text.secondary">Contador indisponível.</Typography>
                )}
              </CardContent></Card>
            </Grid>
          </Grid>

          <Grid container spacing={2}>
            <Grid item xs={12} md={6}>
              <Paper sx={{ p: 2 }}>
                <Typography variant="h6" gutterBottom>Contemplados por campanha</Typography>
                {historyYears.length === 0 ? (
                  <Alert severity="info">Histórico ainda não importado.</Alert>
                ) : (
                  <TableContainer>
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell>Ano</TableCell>
                          <TableCell align="right">Famílias contempladas</TableCell>
                          <TableCell align="right">Sem CPF</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {historyYears.map((y) => (
                          <TableRow key={y}>
                            <TableCell>{y}</TableCell>
                            <TableCell align="right">{overview.historicalByYear[y]}</TableCell>
                            <TableCell align="right">{overview.historicalWithoutCpfByYear[y] ?? 0}</TableCell>
                          </TableRow>
                        ))}
                        <TableRow>
                          <TableCell><strong>{overview.campaignYear} (cadastradas)</strong></TableCell>
                          <TableCell align="right"><strong>{overview.registeredThisCampaign}</strong></TableCell>
                          <TableCell align="right">—</TableCell>
                        </TableRow>
                      </TableBody>
                    </Table>
                  </TableContainer>
                )}
              </Paper>
            </Grid>
            <Grid item xs={12} md={6}>
              <Paper sx={{ p: 2 }}>
                <Typography variant="h6" gutterBottom>Distribuições em {overview.campaignYear}</Typography>
                <Stack spacing={1}>
                  <Stack direction="row" justifyContent="space-between">
                    <Typography variant="body2">Distribuições confirmadas</Typography>
                    <Typography variant="body2" fontWeight={600}>{summary.distributions.confirmed}</Typography>
                  </Stack>
                  <Stack direction="row" justifyContent="space-between">
                    <Typography variant="body2">Famílias atendidas</Typography>
                    <Typography variant="body2" fontWeight={600}>{summary.distributions.distinctFamiliesServed}</Typography>
                  </Stack>
                  <Stack direction="row" justifyContent="space-between">
                    <Typography variant="body2">Itens entregues</Typography>
                    <Typography variant="body2" fontWeight={600}>{summary.distributions.itemsDistributed}</Typography>
                  </Stack>
                </Stack>
                {summary.distributions.topProducts.length > 0 && (
                  <>
                    <Divider sx={{ my: 1.5 }} />
                    <Typography variant="subtitle2" gutterBottom>Itens mais distribuídos</Typography>
                    {summary.distributions.topProducts.slice(0, 8).map((p) => (
                      <Stack key={p.productId} direction="row" justifyContent="space-between">
                        <Typography variant="body2">{p.productName}</Typography>
                        <Typography variant="body2" fontWeight={600}>{p.quantity} {p.unit}</Typography>
                      </Stack>
                    ))}
                  </>
                )}
              </Paper>
            </Grid>
          </Grid>
        </Stack>
      ) : null}
    </PageShell>
  )
}

export default ReportsPage
