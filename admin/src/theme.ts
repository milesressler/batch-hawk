import { createTheme } from '@mantine/core';
import type { MantineColorsTuple } from '@mantine/core';

const amber: MantineColorsTuple = [
  '#fff8e1', '#ffecb3', '#ffe082', '#ffd54f', '#ffca28',
  '#ffc107', '#ffb300', '#ffa000', '#ff8f00', '#ff6f00',
];

export const theme = createTheme({
  primaryColor: 'amber',
  primaryShade: 7,
  colors: { amber },
  fontFamily: 'system-ui, sans-serif',
  fontFamilyMonospace: '"DM Mono", ui-monospace, monospace',
  headings: {
    fontFamily: '"Playfair Display", Georgia, serif',
  },
  defaultRadius: 'md',
});