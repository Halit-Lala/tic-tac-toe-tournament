import { HttpContext, HttpContextToken } from '@angular/common/http';

/** Client-side flags read by interceptors. They are never sent to the server. */
export const SHOW_LOADER = new HttpContextToken<boolean>(() => false);
export const NOTIFY_ERRORS = new HttpContextToken<boolean>(() => true);

export interface RequestOptions {
  loader?: boolean;
  notifyErrors?: boolean;
}

export const requestContext = (options: RequestOptions = {}): HttpContext =>
  new HttpContext()
    .set(SHOW_LOADER, options.loader ?? false)
    .set(NOTIFY_ERRORS, options.notifyErrors ?? false);