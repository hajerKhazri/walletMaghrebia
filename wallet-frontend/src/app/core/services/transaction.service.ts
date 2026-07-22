import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, Transaction, TransactionCreateRequest, ValidateOtpRequest, TransactionFilter, Page } from '../models';

@Injectable({
  providedIn: 'root'
})
export class TransactionService {
  private http = inject(HttpClient);
  private readonly API_URL = 'http://localhost:8081/api/transactions';

  createTransaction(request: TransactionCreateRequest): Observable<ApiResponse<Transaction>> {
    return this.http.post<ApiResponse<Transaction>>(this.API_URL, request);
  }

  getTransaction(id: number): Observable<ApiResponse<Transaction>> {
    return this.http.get<ApiResponse<Transaction>>(`${this.API_URL}/${id}`);
  }

  // ✅ Méthode avec filtres et pagination
  getAllTransactions(filters: TransactionFilter, page: number = 0, size: number = 20): Observable<ApiResponse<Page<Transaction>>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    // Ajouter les filtres
    if (filters.status) {
      params = params.set('status', filters.status);
    }
    if (filters.mobileNumber) {
      params = params.set('mobileNumber', filters.mobileNumber);
    }
    if (filters.fromDate) {
      params = params.set('fromDate', filters.fromDate);
    }
    if (filters.toDate) {
      params = params.set('toDate', filters.toDate);
    }

    return this.http.get<ApiResponse<Page<Transaction>>>(this.API_URL, { params });
  }

  getMyTransactions(page: number = 0, size: number = 20): Observable<ApiResponse<Page<Transaction>>> {
    return this.http.get<ApiResponse<Page<Transaction>>>(`${this.API_URL}/my?page=${page}&size=${size}`);
  }

  getPendingTransactions(page: number = 0, size: number = 20): Observable<ApiResponse<Page<Transaction>>> {
    return this.http.get<ApiResponse<Page<Transaction>>>(
      `${this.API_URL}/pending?page=${page}&size=${size}`
    );
  }

  validateTransaction(id: number, payload: any): Observable<ApiResponse<Transaction>> {
    return this.http.post<ApiResponse<Transaction>>(`${this.API_URL}/${id}/validate`, payload);
  }

  cancelTransaction(id: number): Observable<ApiResponse<Transaction>> {
    return this.http.post<ApiResponse<Transaction>>(`${this.API_URL}/${id}/cancel`, {});
  }

  rejectTransaction(id: number, reason: string): Observable<ApiResponse<Transaction>> {
    return this.http.post<ApiResponse<Transaction>>(`${this.API_URL}/${id}/reject?reason=${reason}`, {});
  }

  initiateValidation(id: number): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.API_URL}/${id}/initiate-validation`, {});
  }

  updateTransaction(id: number, data: { remarks: string }): Observable<ApiResponse<Transaction>> {
    return this.http.put<ApiResponse<Transaction>>(`${this.API_URL}/${id}`, data);
  }

  deleteTransaction(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.API_URL}/${id}`);
  }
}