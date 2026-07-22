import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule],
  template: `
    <footer class="footer">
      <div class="footer-container">
        <p>&copy; {{ currentYear }} Wallet Maghrebia - Tous droits réservés</p>
        <p class="footer-version">Version 1.0.0</p>
      </div>
    </footer>
  `,
  styles: [`
    .footer {
      background: #f8f9fa;
      border-top: 1px solid #e9ecef;
      padding: 16px 20px;
      text-align: center;
      font-size: 14px;
      color: #6c757d;
    }
    .footer-container {
      max-width: 1200px;
      margin: 0 auto;
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;
    }
    .footer-version {
      font-size: 12px;
      color: #adb5bd;
    }
    @media (max-width: 576px) {
      .footer-container { flex-direction: column; }
    }
  `]
})
export class FooterComponent {
  currentYear = new Date().getFullYear();
}