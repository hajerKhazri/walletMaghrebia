import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, VerifyWalletRequest, VerifyWalletResponse } from '../models';

@Injectable({
  providedIn: 'root'
})
export class WalletService {
  private http = inject(HttpClient);
  private readonly API_URL = 'http://localhost:8081/api/wallet';

  verifyWallet(request: VerifyWalletRequest): Observable<ApiResponse<VerifyWalletResponse>> {
    return this.http.post<ApiResponse<VerifyWalletResponse>>(`${this.API_URL}/verify`, request);
  }
}