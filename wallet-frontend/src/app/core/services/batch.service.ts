import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, Batch, ValidateOtpRequest, Transaction } from '../models';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class BatchService {
  private http = inject(HttpClient);
  private readonly API_URL = `${environment.apiUrl}/batches`;

  uploadBatch(file: File): Observable<ApiResponse<Batch>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ApiResponse<Batch>>(this.API_URL, formData);
  }

  getBatch(id: number): Observable<ApiResponse<Batch>> {
    return this.http.get<ApiResponse<Batch>>(`${this.API_URL}/${id}`);
  }

  getAllBatches(page: number = 0, size: number = 20): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.API_URL}?page=${page}&size=${size}`);
  }

  getMyBatches(page: number = 0, size: number = 20): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.API_URL}/my?page=${page}&size=${size}`);
  }

  validateBatch(id: number, request: ValidateOtpRequest): Observable<ApiResponse<Batch>> {
    return this.http.post<ApiResponse<Batch>>(`${this.API_URL}/${id}/validate`, request);
  }

  rejectBatch(id: number, reason: string): Observable<ApiResponse<Batch>> {
    return this.http.post<ApiResponse<Batch>>(`${this.API_URL}/${id}/reject?reason=${reason}`, {});
  }

  exportBatches(): Observable<Blob> {
    return this.http.get(`${this.API_URL}/export`, { responseType: 'blob' });
  }

  updateBatch(id: number, data: { filename: string }): Observable<ApiResponse<Batch>> {
    return this.http.put<ApiResponse<Batch>>(`${this.API_URL}/${id}`, data);
  }

  deleteBatch(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.API_URL}/${id}`);
  }

  initiateValidation(id: number): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.API_URL}/${id}/initiate-validation`, {});
  }

  // ✅ Récupérer les transactions d'un batch
  getBatchTransactions(id: number): Observable<ApiResponse<Transaction[]>> {
    return this.http.get<ApiResponse<Transaction[]>>(`${this.API_URL}/${id}/transactions`);
  }
}