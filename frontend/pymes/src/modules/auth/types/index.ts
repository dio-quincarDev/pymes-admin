export interface User {
  id: string;
  email: string;
  name: string;
  role?: string;
  tenantId?: string;
  plan?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
  activeTenant?: { id: string; name: string; slug: string; plan: string };
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  companyName?: string;
  companySlug?: string;
  invitationToken?: string;
}

export interface ApiResponse<T> {
  data: T;
  codigo: string;
  mensaje: string;
  timestamp: string;
}

export interface LogoutResponse {
  success: boolean;
  message: string;
  timestamp: string;
  allSessionsRevoked: boolean;
}

export interface InvitationResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
  tenant: {
    name: string;
  };
}

export interface InvitationInfo {
  email: string;
  tenantName: string;
}
