// core/http/error.interceptor.ts
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ApiError } from './api-error.model';
import { NOTIFY_ERRORS } from './request-context';
import { NotificationService } from '../service/notification.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notifications = inject(NotificationService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (req.context.get(NOTIFY_ERRORS)) {
        notifications.error(getErrorMessage(error));
      }
      return throwError(() => error);
    }),
  );
};

export function getErrorMessage(error: HttpErrorResponse): string {
  if (error.status === 0) {
    return 'Cannot reach the server. Please try again.';
  }
  const body = error.error as Partial<ApiError> | null;
  return body?.detail ?? 'Something went wrong. Please try again.';
}