import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { LeaderboardEntry } from '../models/leaderboard.model';
import { PageResponse } from '../../../shared/model/page.model';
import { requestContext, RequestOptions } from '../../../core/http/request-context';

@Injectable({
  providedIn: 'root',
})
export class LeaderboardApiService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = `${environment.apiUrl}/leaderboard`;

  getLeaderboard(
    pageNumber: number,
    pageSize: number,
    options?: RequestOptions
  ): Observable<PageResponse<LeaderboardEntry>> {
    return this.http.get<PageResponse<LeaderboardEntry>>(
      this.apiUrl,
      { 
        params: { pageNumber: pageNumber.toString(), pageSize: pageSize.toString() },
        context: requestContext({ loader: true, notifyErrors: true, ...options })
      }
    );
  }
}