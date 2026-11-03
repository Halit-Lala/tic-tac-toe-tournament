import { computed, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Subject, catchError, exhaustMap, finalize, switchMap, tap } from 'rxjs';
import { TOTAL_GAMES, Tournament } from '../models/tournament.model';
import { TournamentApiService } from '../services/tournament-api.service';

@Injectable({ providedIn: 'root' })
export class TournamentStore {
    private readonly tournamentApi = inject(TournamentApiService);

    private readonly _tournament = signal<Tournament | null>(null);
    private readonly _loading = signal(false);
    private readonly _busy = signal(false);
    private readonly _loadFailed = signal(false);

    readonly tournament = this._tournament.asReadonly();
    readonly loading = this._loading.asReadonly();
    readonly busy = this._busy.asReadonly();
    readonly loadFailed = this._loadFailed.asReadonly();

    readonly lastGame = computed(
        () => this._tournament()?.games.at(-1) ?? null);

    readonly currentGame = computed(() => {
        const lastGame = this.lastGame();
        return lastGame?.status === 'IN_PROGRESS' ? lastGame : null;
    });

    // readonly currentGame = computed(
    //     () => this._tournament()?.games.find((game) => game.status === 'IN_PROGRESS') ?? null,
    // );
    
    readonly isCompleted = computed(
        () => this._tournament()?.status === 'COMPLETED');
    
    readonly awaitingNextGame = computed(
        () => this._tournament()?.status === 'IN_PROGRESS' && this.currentGame() === null);
    
    readonly progress = computed(() => {
        const tournament = this._tournament();
        return tournament ? { gameNumber: tournament.games.length, totalGames: TOTAL_GAMES[tournament.format] } : null;
    });

    private readonly load$ = new Subject<number>();
    private readonly move$ = new Subject<{ row: number; col: number }>();
    private readonly nextGame$ = new Subject<void>();

    constructor() {
        this.load$.pipe(
            tap(() => {
            this._loading.set(true);
            this._loadFailed.set(false);
            }),
            switchMap((id) =>
                this.tournamentApi.get(id).pipe(
                    tap((tournament) => this._tournament.set(tournament)),
                    catchError(() => {
                        this._tournament.set(null);
                        this._loadFailed.set(true);
                        return EMPTY;
                    }),
                    finalize(() => this._loading.set(false)),
                ),
            ),
            takeUntilDestroyed(),
        ).subscribe();

        this.move$.pipe(
            exhaustMap(({ row, col }) => {
                const tournament = this._tournament();
                const game = this.currentGame();
                if (!tournament || !game?.nextSymbol) {
                    return EMPTY;
                }
                this._busy.set(true);
                return this.tournamentApi.playMove(tournament.id, game.gameNumber, { row, col, symbol: game.nextSymbol }).pipe(
                        tap((updated) => this._tournament.set(updated)),
                        catchError(() => {
                            this.load$.next(tournament.id);
                            return EMPTY;
                        }),
                        finalize(() => this._busy.set(false)),
                    );
            }),
            takeUntilDestroyed(),
        ).subscribe();

        this.nextGame$.pipe(
            exhaustMap(() => {
                const tournament = this._tournament();
                if (!tournament) {
                    return EMPTY;
                }
                this._busy.set(true);
                return this.tournamentApi.startNextGame(tournament.id).pipe(
                    tap((updated) => this._tournament.set(updated)),
                    catchError(() => EMPTY),
                    finalize(() => this._busy.set(false)),
                );
            }),
            takeUntilDestroyed(),
        ).subscribe();
    }

    load(id: number): void {
        this.load$.next(id);
    }

    playMove(row: number, col: number): void {
        this.move$.next({ row, col });
    }

    startNextGame(): void {
        this.nextGame$.next();
    }

    reset(): void {
        this._tournament.set(null);
        this._loadFailed.set(false);
    }
}