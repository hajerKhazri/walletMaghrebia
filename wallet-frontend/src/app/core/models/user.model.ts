export enum Role {
  INITIATEUR = 'INITIATEUR',
  VALIDATEUR = 'VALIDATEUR',
  FINANCE = 'FINANCE',
  ADMIN = 'ADMIN'
}

export interface User {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
  active: boolean;
  createdAt: string;
  lastLogin: string | null;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userId: number;
  username: string;
  email: string;
  role: Role;
}

export interface AuthState {
  isAuthenticated: boolean;
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
}