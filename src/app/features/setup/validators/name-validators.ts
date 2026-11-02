import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

const normalize = (value: unknown): string =>
    typeof value === 'string' ? value.trim().toLowerCase() : '';

export const notBlankValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null =>
    typeof control.value === 'string' && control.value.length > 0 && control.value.trim() === ''
        ? { blank: true }
        : null;

export const distinctNamesValidator: ValidatorFn = (group: AbstractControl): ValidationErrors | null => {
    const first = normalize(group.get('playerXName')?.value);
    const second = normalize(group.get('playerOName')?.value);
    return first && second && first === second ? { sameNames: true } : null;
};