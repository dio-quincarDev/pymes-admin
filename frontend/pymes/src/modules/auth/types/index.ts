export interface User {
  id: string;
  email: string;
  nombre: string;
  role?: string;
  tenantId?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  companyName: string;
  companySlug: string;
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
