import { Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from '@/features/auth/AuthProvider';
import { LoginPage } from '@/features/auth/LoginPage';
import { ProtectedRoute } from '@/features/auth/ProtectedRoute';
import { RegisterPage } from '@/features/auth/RegisterPage';
import { ChatPage } from '@/features/chat/ChatPage';
import { NewConversationPage } from '@/features/conversations/NewConversationPage';
import { AppLayout } from './layout/AppLayout';
import { EmptyChat } from './layout/EmptyChat';

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/cadastro" element={<RegisterPage />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route index element={<EmptyChat />} />
            <Route path="conversas/nova" element={<NewConversationPage />} />
            <Route path="conversas/:id" element={<ChatPage />} />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AuthProvider>
  );
}
