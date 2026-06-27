import { Navigate, Route, Routes } from 'react-router-dom';
import { LoginPage, SignupPage } from './features/auth/AuthPages';
import { SelfQuotePage } from './features/parts/PartsPages';
import { BuildResultPage, ChangePartPage, HomePage, MyQuotesPage, RequirementPage } from './features/quote/QuotePages';
import { SupportNewPage, SupportTicketPage } from './features/support/SupportPages';
import { AdminDashboardPage, AdminPartsPage, AdminTicketDetailPage, AdminTicketsPage, AgentSessionAdminPage, RagEvidenceAdminPage, ToolInvocationAdminPage } from './features/admin/AdminPages';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/requirements/new" element={<RequirementPage />} />
      <Route path="/builds/:buildId" element={<BuildResultPage />} />
      <Route path="/self-quote" element={<SelfQuotePage />} />
      <Route path="/builds/:buildId/change-part" element={<ChangePartPage />} />
      <Route path="/my/quotes" element={<MyQuotesPage />} />
      <Route path="/support/new" element={<SupportNewPage />} />
      <Route path="/support/:ticketId" element={<SupportTicketPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/admin" element={<AdminDashboardPage />} />
      <Route path="/admin/agent-sessions/:id" element={<AgentSessionAdminPage />} />
      <Route path="/admin/tool-invocations/:id" element={<ToolInvocationAdminPage />} />
      <Route path="/admin/rag-evidence/:id" element={<RagEvidenceAdminPage />} />
      <Route path="/admin/parts" element={<AdminPartsPage />} />
      <Route path="/admin/as-tickets" element={<AdminTicketsPage />} />
      <Route path="/admin/as-tickets/:ticketId" element={<AdminTicketDetailPage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
