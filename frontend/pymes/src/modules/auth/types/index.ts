export interface User {
  id: string;
  email: string;
  name: string;
  role?: string | undefined;
  tenantId?: string | undefined;
  plan?: string | undefined;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
  activeTenant?: { id: string; name: string; slug: string; plan?: string };
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

export interface InvitationInfo {
  email: string;
  tenantName: string;
}

export interface InvitationRegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface MemberResponse {
  user: {
    id: string;
    email: string;
    name: string;
    pictureUrl?: string;
  };
  role: string;
  accepted: boolean;
  joinedAt: string;
}
