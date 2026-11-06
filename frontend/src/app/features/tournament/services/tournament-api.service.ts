// features/tournament/data-access/tournament-api.service.ts
import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { NewTournamentRequest } from '../../setup/models/setup.model';
import { CreatedTournament, PlayMoveRequest, SeatAccess, Tournament } from '../models/tournament.model';
import { requestContext, RequestOptions } from '../../../core/http/request-context';
import { PageResponse } from '../../../shared/model/page.model';
import { TournamentHistoryEntry } from '../../history/models/tournament-history.model';
import { SeatStorage } from './seat-storage.service';

@Injectable({ providedIn: 'root' })
export class TournamentApiService {
  private readonly http = inject(HttpClient);
  private readonly seatStorage = inject(SeatStorage);
  private readonly baseUrl = '/api/tournaments';

  createTournament(request: NewTournamentRequest, options?: RequestOptions): Observable<CreatedTournament> {
    return this.http.post<CreatedTournament>(
      this.baseUrl,
      request,
      { context: requestContext({ loader: true, notifyErrors: true, ...options })}
    );
  }

  joinTournament(id: number, inviteToken: string, options: RequestOptions = {}): Observable<SeatAccess> {
    return this.http.post<SeatAccess>(
      `${this.baseUrl}/${id}/join`,
      { inviteToken },
      { context: requestContext({ loader: true, notifyErrors: true, ...options })}
    );
  }

  get(id: number, options?: RequestOptions): Observable<Tournament> {
    return this.http.get<Tournament>(
      `${this.baseUrl}/${id}`,
      { context: requestContext({ loader: true, notifyErrors: true, ...options })}
    );
  }

  getTournamentHistory(
    pageNumber: number,
    pageSize: number,
    options?: RequestOptions
  ): Observable<PageResponse<TournamentHistoryEntry>> {
    return this.http.get<PageResponse<TournamentHistoryEntry>>(
      `${this.baseUrl}`,
      {
        params: { pageNumber: pageNumber.toString(), pageSize: pageSize.toString() },
        context: requestContext({ loader: true, notifyErrors: true, ...options })
      }
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
      {
        headers: this.seatHeaders(id),
        context: requestContext({ loader: true, notifyErrors: true, ...options })
      }
    );
  }

  startNextGame(
    id: number,
    options?: RequestOptions
  ): Observable<Tournament> {
    return this.http.post<Tournament>(
      `${this.baseUrl}/${id}/games/next`,
      null,
      { 
        headers: this.seatHeaders(id),
        context: requestContext({ loader: true, notifyErrors: true, ...options })
      }
    );
  }

  private seatHeaders(id: number): Record<string, string> {
    const access = this.seatStorage.getSeatAccess(id);
    return access ? { 'X-Seat-Token': access.seatToken } : {};
  }
}