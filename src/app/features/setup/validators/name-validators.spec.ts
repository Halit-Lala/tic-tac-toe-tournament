import { FormControl, FormGroup } from '@angular/forms';
import { describe, expect, it } from 'vitest';
import { distinctNamesValidator, notBlankValidator } from './name-validators';

describe('distinctNamesValidator', () => {
  const run = (first: string, second: string) =>
    distinctNamesValidator(
      new FormGroup({ playerXName: new FormControl(first), playerOName: new FormControl(second) }),
    );

  it('rejects identical names', () => 
    expect(run('Ann', 'Ann')).toEqual({ sameNames: true }));
  
  it('ignores case', () => 
    expect(run('Ann', 'ANN')).toEqual({ sameNames: true }));
  
  it('ignores leading and trailing spaces', () => 
    expect(run('  Ann ', 'ann')).toEqual({ sameNames: true }));
  
  it('accepts different names', () => 
    expect(run('Ann', 'Bob')).toBeNull());
  
  it('does not report when a name is empty', () => 
    expect(run('', '')).toBeNull());
  
  it('keeps inner spaces significant', () => 
    expect(run('Ann Lee', 'Ann  Lee')).toBeNull());
});

describe('notBlankValidator', () => {
  it('rejects whitespace-only values', () =>
    expect(notBlankValidator(new FormControl('   '))).toEqual({ blank: true }));
  
  it('accepts normal and empty values', () => {
    expect(notBlankValidator(new FormControl('Ann'))).toBeNull();
    expect(notBlankValidator(new FormControl(''))).toBeNull();
  });
});