import { inject, Injectable } from '@angular/core';
import { MatSnackBar, MatSnackBarConfig } from '@angular/material/snack-bar';

type NotificationType = 'info' | 'success' | 'error';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly snackBar = inject(MatSnackBar);

  info(message: string): void {
    this.show(message, 'info');
  }

  success(message: string): void {
    this.show(message, 'success');
  }

  error(message: string): void {
    this.show(message, 'error', 6000);
  }

  private show(message: string, type: NotificationType, duration = 4000): void {
    const config: MatSnackBarConfig = {
      duration,
      panelClass: ['snackbar', `snackbar-${type}`, 'custom-snackbar'],
      horizontalPosition: 'end',
      verticalPosition: 'top',
    };
    this.snackBar.open(message, 'Close', config);
  }
}