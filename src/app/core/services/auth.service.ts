import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';
import { ApiResponse, LoginRequest, LoginResponse, User, AuthState } from '../models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private platformId = inject(PLATFORM_ID);
  private readonly API_URL = 'http://localhost:8081/api/auth';
  
  private authStateSubject = new BehaviorSubject<AuthState>({
    isAuthenticated: false,
    user: null,
    accessToken: null,
    refreshToken: null
  });
  authState$ = this.authStateSubject.asObservable();

  constructor() {
    this.loadStoredAuth();
  }

  login(loginRequest: LoginRequest): Observable<ApiResponse<LoginResponse>> {
    return this.http.post<ApiResponse<LoginResponse>>(`${this.API_URL}/login`, loginRequest)
      .pipe(
        tap(response => {
          if (response.data) {
            this.setAuthData(response.data);
          }
        })
      );
  }

  refreshToken(): Observable<ApiResponse<LoginResponse>> {
    const refreshToken = this.getRefreshToken();
    return this.http.post<ApiResponse<LoginResponse>>(`${this.API_URL}/refresh`, { refreshToken })
      .pipe(
        tap(response => {
          if (response.data) {
            this.setAuthData(response.data);
          }
        })
      );
  }

  logout(): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.API_URL}/logout`, {});
  }

  private setAuthData(loginResponse: LoginResponse): void {
    const user: User = {
      id: loginResponse.userId,
      username: loginResponse.username,
      email: loginResponse.email,
      role: loginResponse.role,
      firstName: '',
      lastName: '',
      active: true,
      createdAt: new Date().toISOString(),
      lastLogin: null
    };

    this.authStateSubject.next({
      isAuthenticated: true,
      accessToken: loginResponse.accessToken,
      refreshToken: loginResponse.refreshToken,
      user
    });

    // ✅ Vérifier qu'on est dans le navigateur avant d'utiliser localStorage
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem('accessToken', loginResponse.accessToken);
      localStorage.setItem('refreshToken', loginResponse.refreshToken);
      localStorage.setItem('user', JSON.stringify(user));
    }
  }

  clearAuth(): void {
    this.authStateSubject.next({
      isAuthenticated: false,
      user: null,
      accessToken: null,
      refreshToken: null
    });

    // ✅ Vérifier qu'on est dans le navigateur avant d'utiliser localStorage
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
    }
  }

  private loadStoredAuth(): void {
    // ✅ Vérifier qu'on est dans le navigateur avant d'utiliser localStorage
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    const accessToken = localStorage.getItem('accessToken');
    const refreshToken = localStorage.getItem('refreshToken');
    const userStr = localStorage.getItem('user');

    if (accessToken && refreshToken && userStr) {
      try {
        const user = JSON.parse(userStr);
        this.authStateSubject.next({
          isAuthenticated: true,
          accessToken,
          refreshToken,
          user
        });
      } catch (e) {
        this.clearAuth();
      }
    }
  }

  getAccessToken(): string | null {
    return this.authStateSubject.value.accessToken;
  }

  getRefreshToken(): string | null {
    return this.authStateSubject.value.refreshToken;
  }

  getCurrentUser(): User | null {
    return this.authStateSubject.value.user;
  }

  isAuthenticated(): boolean {
    return this.authStateSubject.value.isAuthenticated;
  }

  hasRole(role: string): boolean {
    const user = this.getCurrentUser();
    if (!user) return false;
    if (user.role === 'ADMIN') return true;
    return user.role === role;
  }
}