export const state = $state({
  domRingHead: 0,
  listTranslateY: 0,
});

export const resetScroll = () => {
  state.domRingHead = 0;
  state.listTranslateY = 0;
};
