import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TournamentStore } from './tournament.store';
import { Game, Tournament } from '../models/tournament.model';
import { PlayerSymbol } from '../../player/model/player.model';

const URL = '/api/tournaments';

function game(overrides: Partial<Game> = {}): Game {
    return {
        id: 1,
        gameNumber: 1,
        startingSymbol: 'X',
        nextSymbol: 'X',
        board: [
            [null, null, null],
            [null, null, null],
            [null, null, null],
        ],
        status: 'IN_PROGRESS',
        outcome: null,
        ...overrides,
    };
}

function tournament(overrides: Partial<Tournament> = {}): Tournament {
    return {
        id: 1,
        playerX: { id: 1, name: 'Ann' },
        playerO: { id: 2, name: 'Bob' },
        format: 'BEST_OF_3',
        status: 'IN_PROGRESS',
        winner: null,
        score: { playerXWins: 0, playerOWins: 0, draws: 0 },
        games: [game()],
        ...overrides,
    };
}

describe('TournamentStore', () => {
    let store: TournamentStore;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting()],
        });
        store = TestBed.inject(TournamentStore);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => httpMock.verify());

    /** Loads a tournament into the store through the real load flow. */
    function givenLoaded(t: Tournament): void {
        store.load(t.id);
        httpMock.expectOne(`${URL}/${t.id}`).flush(t);
    }

    describe('initial state', () => {
        it('is empty and idle', () => {
            expect(store.tournament()).toBeNull();
            expect(store.loading()).toBe(false);
            expect(store.busy()).toBe(false);
            expect(store.loadFailed()).toBe(false);
            expect(store.currentGame()).toBeNull();
            expect(store.progress()).toBeNull();
            expect(store.isCompleted()).toBe(false);
            expect(store.awaitingNextGame()).toBe(false);
        });
    });

    describe('load', () => {
        it('requests the tournament and stores the response', () => {
            store.load(1);

            const req = httpMock.expectOne(`${URL}/1`);
            expect(req.request.method).toBe('GET');
            req.flush(tournament());

            expect(store.tournament()?.id).toBe(1);
        });

        it('is loading only while the request is in flight', () => {
            store.load(1);
            expect(store.loading()).toBe(true);

            httpMock.expectOne(`${URL}/1`).flush(tournament());
            expect(store.loading()).toBe(false);
        });

        it('clears the tournament and flags the failure when the request fails', () => {
            givenLoaded(tournament({ id: 1 }));

            store.load(2);
            httpMock.expectOne(`${URL}/2`).flush(
                { status: 404, detail: 'Tournament not found with id: 2' },
                { status: 404, statusText: 'Not Found' },
            );

            expect(store.tournament()).toBeNull();
            expect(store.loadFailed()).toBe(true);
            expect(store.loading()).toBe(false);
        });

        it('resets the failure flag on the next load', () => {
            store.load(2);
            httpMock.expectOne(`${URL}/2`).flush('', { status: 404, statusText: 'Not Found' });
            expect(store.loadFailed()).toBe(true);

            store.load(1);
            expect(store.loadFailed()).toBe(false);
            httpMock.expectOne(`${URL}/1`).flush(tournament());
        });

        it('cancels the previous request when another tournament is loaded', () => {
            store.load(1);
            const first = httpMock.expectOne(`${URL}/1`);

            store.load(2);
            const second = httpMock.expectOne(`${URL}/2`);

            expect(first.cancelled).toBe(true);

            second.flush(tournament({ id: 2 }));
            expect(store.tournament()?.id).toBe(2);
        });
    });

    describe('derived state', () => {
        it('exposes the game in progress as the current game', () => {
            givenLoaded(
                tournament({
                games: [
                    game({ id: 1, gameNumber: 1, status: 'COMPLETED', outcome: 'X_WINS', nextSymbol: null }),
                    game({ id: 2, gameNumber: 2, startingSymbol: 'O', nextSymbol: 'O' }),
                ],
                }),
            );

            expect(store.currentGame()?.gameNumber).toBe(2);
            expect(store.lastGame()?.gameNumber).toBe(2);
            expect(store.awaitingNextGame()).toBe(false);
        });

        it('is awaiting the next game when the tournament runs but no game does', () => {
            givenLoaded(
                tournament({
                games: [game({ status: 'COMPLETED', outcome: 'X_WINS', nextSymbol: null })],
                }),
            );

            expect(store.currentGame()).toBeNull();
            expect(store.awaitingNextGame()).toBe(true);
            expect(store.lastGame()?.outcome).toBe('X_WINS');
        });

        it('is completed, and not awaiting a game, when the tournament is over', () => {
            givenLoaded(
                tournament({
                status: 'COMPLETED',
                winner: { id: 1, name: 'Ann' },
                games: [game({ status: 'COMPLETED', outcome: 'X_WINS', nextSymbol: null })],
                }),
            );

            expect(store.isCompleted()).toBe(true);
            expect(store.awaitingNextGame()).toBe(false);
        });

        it.each([
        ['BEST_OF_1', 1],
        ['BEST_OF_3', 3],
        ['BEST_OF_5', 5],
        ] as const)('reports progress for %s', (format, total) => {
            givenLoaded(tournament({ format }));

            expect(store.progress()).toEqual({ gameNumber: 1, totalGames: total });
        });
    });

    describe('playMove', () => {
        it('sends the move with the symbol the server expects next', () => {
            givenLoaded(tournament({ games: [game({ gameNumber: 1, nextSymbol: 'O' })] }));

            store.playMove(1, 2);

            const req = httpMock.expectOne(`${URL}/1/games/1/moves`);
            expect(req.request.method).toBe('POST');
            expect(req.request.body).toEqual({ row: 1, col: 2, symbol: 'O' });
            req.flush(tournament());
        });

        it('replaces the state with the server response', () => {
            givenLoaded(tournament());

            store.playMove(0, 0);
            httpMock.expectOne(`${URL}/1/games/1/moves`).flush(
                tournament({
                score: { playerXWins: 1, playerOWins: 0, draws: 0 },
                games: [
                    game({
                    status: 'COMPLETED',
                    outcome: 'X_WINS',
                    nextSymbol: null,
                    board: [
                        ['X', 'X', 'X'],
                        ['O', 'O', null],
                        [null, null, null],
                    ],
                    }),
                ],
                }),
            );

            expect(store.tournament()?.score.playerXWins).toBe(1);
            expect(store.currentGame()).toBeNull();
            expect(store.lastGame()?.outcome).toBe('X_WINS');
        });

        it('is busy only while the move is in flight', () => {
            givenLoaded(tournament());

            store.playMove(0, 0);
            expect(store.busy()).toBe(true);

            httpMock.expectOne(`${URL}/1/games/1/moves`).flush(tournament());
            expect(store.busy()).toBe(false);
        });

        it('ignores further moves while one is in flight', () => {
            givenLoaded(tournament());

            store.playMove(0, 0);
            store.playMove(1, 1);
            store.playMove(2, 2);

            const requests = httpMock.match(`${URL}/1/games/1/moves`);
            expect(requests).toHaveLength(1);
            expect(requests[0].request.body).toEqual({ row: 0, col: 0, symbol: 'X' });
            requests[0].flush(tournament());
        });

        it('accepts a new move after the previous one finished', () => {
            givenLoaded(tournament());

            store.playMove(0, 0);
            httpMock.expectOne(`${URL}/1/games/1/moves`).flush(tournament());

            store.playMove(1, 1);
            httpMock.expectOne(`${URL}/1/games/1/moves`).flush(tournament());
        });

        it('sends nothing when no tournament is loaded', () => {
            store.playMove(0, 0);

            httpMock.expectNone(() => true);
        });

        it('sends nothing when there is no game in progress', () => {
            givenLoaded(
                tournament({
                games: [game({ status: 'COMPLETED', outcome: 'DRAW', nextSymbol: null })],
                }),
            );

            store.playMove(0, 0);

            httpMock.expectNone(() => true);
        });

        it('resyncs from the server when a move is rejected', () => {
            givenLoaded(tournament());

            store.playMove(0, 0);
            httpMock.expectOne(`${URL}/1/games/1/moves`).flush(
                { status: 409, detail: 'Cell is already occupied' },
                { status: 409, statusText: 'Conflict' },
            );

            expect(store.busy()).toBe(false);

            // the store reloads, so the board shows the real server state
            const reload = httpMock.expectOne(`${URL}/1`);
            expect(reload.request.method).toBe('GET');

            const serverState = tournament({
                games: [
                game({
                    nextSymbol: 'O',
                    board: [
                    ['X', null, null],
                    [null, null, null],
                    [null, null, null],
                    ],
                }),
                ],
            });
            reload.flush(serverState);

            expect(store.tournament()).toEqual(serverState);
        });
    });

    describe('startNextGame', () => {
        const awaiting = () =>
            tournament({
                games: [game({ status: 'COMPLETED', outcome: 'X_WINS', nextSymbol: null })],
            });

        it('requests the next game and stores the response', () => {
            givenLoaded(awaiting());

            store.startNextGame();

            const req = httpMock.expectOne(`${URL}/1/games/next`);
            expect(req.request.method).toBe('POST');
            req.flush(
                tournament({
                games: [
                    game({ status: 'COMPLETED', outcome: 'X_WINS', nextSymbol: null }),
                    game({ id: 2, gameNumber: 2, startingSymbol: 'O', nextSymbol: 'O' }),
                ],
                }),
            );

            expect(store.currentGame()?.gameNumber).toBe(2);
            expect(store.currentGame()?.startingSymbol).toBe('O');
            expect(store.awaitingNextGame()).toBe(false);
        });

        it('is busy only while the request is in flight', () => {
            givenLoaded(awaiting());

            store.startNextGame();
            expect(store.busy()).toBe(true);

            httpMock.expectOne(`${URL}/1/games/next`).flush(awaiting());
            expect(store.busy()).toBe(false);
        });

        it('ignores repeated calls while a request is in flight', () => {
            givenLoaded(awaiting());

            store.startNextGame();
            store.startNextGame();

            expect(httpMock.match(`${URL}/1/games/next`)).toHaveLength(1);
        });

        it('keeps the state and stops being busy when the request fails', () => {
            const before = awaiting();
            givenLoaded(before);

            store.startNextGame();
            httpMock.expectOne(`${URL}/1/games/next`).flush(
                { status: 409, detail: 'The current game is still in progress' },
                { status: 409, statusText: 'Conflict' },
            );

            expect(store.busy()).toBe(false);
            expect(store.tournament()).toEqual(before);
        });

        it('sends nothing when no tournament is loaded', () => {
            store.startNextGame();

            httpMock.expectNone(() => true);
        });
    });

    describe('reset', () => {
        it('clears the tournament and the failure flag', () => {
            givenLoaded(tournament());

            store.reset();

            expect(store.tournament()).toBeNull();
            expect(store.loadFailed()).toBe(false);
            expect(store.currentGame()).toBeNull();
            expect(store.progress()).toBeNull();
        });
    });

    it('keeps symbols typed as the API contract', () => {
        const symbol: PlayerSymbol = 'X';
        expect(['X', 'O']).toContain(symbol);
    });
});