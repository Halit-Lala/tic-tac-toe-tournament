import { Injectable } from "@angular/core";
import { SeatAccess } from "../models/tournament.model";

@Injectable({ providedIn: 'root' })
export class SeatStorage {
  private key = (id: number) => `seat:${id}`;

  getSeatAccess(id: number): SeatAccess | null {
    try {
      const raw = sessionStorage.getItem(this.key(id));
      return raw ? (JSON.parse(raw) as SeatAccess) : null;
    } catch {
      return null;
    }
  }

  storeSeatAccess(id: number, access: SeatAccess): void {
    try { sessionStorage.setItem(this.key(id), JSON.stringify(access)); } catch { /* storage unavailable */ }
  }
}