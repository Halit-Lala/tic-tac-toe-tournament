// features/tournament/data-access/tournament-api.service.ts
import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { NewTournamentRequest } from '../../setup/models/setup.model';
import { PlayMoveRequest, Tournament } from '../models/tournament.model';
import { requestContext, RequestOptions } from '../../../core/http/request-context';

@Injectable({ providedIn: 'root' })
export class TournamentApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/tournaments';

  createTournament(request: NewTournamentRequest, options?: RequestOptions): Observable<Tournament> {
    return this.http.post<Tournament>(
      this.baseUrl,
      request,
      { context: requestContext({ loader: true, notifyErrors: true, ...options })}
    );
  }

  get(id: number, options?: RequestOptions): Observable<Tournament> {
    return this.http.get<Tournament>(
      `${this.baseUrl}/${id}`,
      { context: requestContext({ loader: true, notifyErrors: true, ...options })}
    );
  }

  playMove(
    id: number,
    gameNumber: number,
    move: PlayMoveRequest,
    options?: RequestOptions
  ): Observable<Tournament> {
    return this.http.post<Tournament>(
      `${this.baseUrl}/${id}/games/${gameNumber}/moves`, move,
      { context: requestContext({ loader: true, notifyErrors: true, ...options })}
    );
  }

  startNextGame(
    id: number,
    options?: RequestOptions
  ): Observable<Tournament> {
    return this.http.post<Tournament>(
      `${this.baseUrl}/${id}/games/next`,
      null,
      { context: requestContext({ loader: true, notifyErrors: true, ...options })}
    );
  }
}