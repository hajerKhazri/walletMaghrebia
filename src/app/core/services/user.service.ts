import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, User, Page } from '../models';
import { environment } from '../../environments/environment';
import { map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private http = inject(HttpClient);
  private readonly API_URL = `${environment.apiUrl}/users`;

  getCurrentUser(): Observable<ApiResponse<User>> {
    return this.http.get<ApiResponse<User>>(`${this.API_URL}/me`);
  }

  getAllUsers(page: number = 0, size: number = 20): Observable<ApiResponse<Page<User>>> {
    return this.http.get<ApiResponse<Page<User>>>(`${this.API_URL}?page=${page}&size=${size}`);
  }

  getUserById(id: number): Observable<ApiResponse<User>> {
    return this.http.get<ApiResponse<User>>(`${this.API_URL}/${id}`);
  }

  createUser(user: Partial<User> & { password: string }): Observable<ApiResponse<User>> {
    return this.http.post<ApiResponse<User>>(this.API_URL, user);
  }

  // ✅ Méthode unique de mise à jour (PUT)
  updateUser(id: number, user: Partial<User>): Observable<ApiResponse<User>> {
    return this.http.put<ApiResponse<User>>(`${this.API_URL}/${id}`, user);
  }

  // ✅ Désactiver un utilisateur (DELETE)
  deactivateUser(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.API_URL}/${id}`);
  }

  // ✅ Activer un utilisateur (PATCH /activate)
  activateUser(id: number): Observable<ApiResponse<User>> {
    return this.http.patch<ApiResponse<User>>(`${this.API_URL}/${id}/activate`, {});
  }

  changeUserRole(id: number, role: string): Observable<ApiResponse<User>> {
    return this.http.patch<ApiResponse<User>>(`${this.API_URL}/${id}/role?role=${role}`, {});
  }

  sendPasswordResetEmail(email: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.API_URL}/forgot-password`, { email });
  }

  resetPassword(token: string, password: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.API_URL}/reset-password`, { token, password });
  }

  // ✅ Solde (existant)
  getBalance(): Observable<number> {
    return this.http.get<ApiResponse<number>>(`${this.API_URL}/balance`).pipe(
      map(response => response.data ?? 0)
    );
  }
}